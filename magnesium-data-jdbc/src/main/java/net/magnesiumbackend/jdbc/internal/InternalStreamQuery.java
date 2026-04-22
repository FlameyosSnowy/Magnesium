package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.RowMapper;
import net.magnesiumbackend.jdbc.streaming.CloseableIterator;
import net.magnesiumbackend.jdbc.streaming.StreamQuery;
import net.magnesiumbackend.jdbc.streaming.ThrowingConsumer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Internal implementation of {@link StreamQuery}.
 */
public class InternalStreamQuery implements StreamQuery {

    private final DataSource dataSource;
    private final String sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final Map<Integer, Object> positionalParams = new HashMap<>();
    private int fetchSize = 1000;
    private boolean readOnly = false;

    InternalStreamQuery(DataSource dataSource, String sql) {
        this.dataSource = dataSource;
        this.sql = sql;
    }

    @Override
    public StreamQuery bind(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    @Override
    public StreamQuery bind(int index, Object value) {
        positionalParams.put(index, value);
        return this;
    }

    @Override
    public StreamQuery fetchSize(int size) {
        this.fetchSize = size;
        return this;
    }

    @Override
    public StreamQuery readOnly() {
        this.readOnly = true;
        return this;
    }

    @Override
    public <T> void forEach(RowMapper<T> mapper, ThrowingConsumer<T> consumer) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = prepareStreamingStatement(conn);
             ResultSet rs = stmt.executeQuery()) {

            int rowNum = 0;
            while (rs.next()) {
                rowNum++;
                T row = mapper.mapRow(rs, rowNum);
                consumer.accept(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Streaming query failed: " + sql, e);
        } catch (Exception e) {
            throw new RuntimeException("Consumer failed processing row", e);
        }
    }

    @Override
    public <T> Stream<T> stream(RowMapper<T> mapper) {
        CloseableIterator<T> iterator = iterator(mapper);
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
        return StreamSupport.stream(spliterator, false)
            .onClose(iterator::close);
    }

    @Override
    public <T> CloseableIterator<T> iterator(RowMapper<T> mapper) {
        return new ResultSetIterator<>(dataSource, sql, fetchSize, readOnly, positionalParams, mapper);
    }

    private PreparedStatement prepareStreamingStatement(Connection conn) throws SQLException {
        // For PostgreSQL and streaming, auto-commit must be false
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
        }

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setFetchSize(fetchSize);

        for (Map.Entry<Integer, Object> entry : positionalParams.entrySet()) {
            stmt.setObject(entry.getKey(), entry.getValue());
        }

        return stmt;
    }
}
