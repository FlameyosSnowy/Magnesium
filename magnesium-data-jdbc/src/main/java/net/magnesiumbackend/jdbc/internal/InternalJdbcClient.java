package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.JdbcClient;
import net.magnesiumbackend.jdbc.JdbcQuery;
import net.magnesiumbackend.jdbc.Update;
import net.magnesiumbackend.jdbc.streaming.StreamQuery;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Internal implementation of {@link JdbcClient}.
 */
public class InternalJdbcClient implements JdbcClient {

    private final DataSource dataSource;

    public InternalJdbcClient(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public JdbcQuery query(String sql) {
        return new InternalJdbcQuery(dataSource, sql);
    }

    @Override
    public Update update(String sql) {
        return new InternalUpdate(dataSource, sql);
    }

    @Override
    public StreamQuery stream(String sql) {
        return new InternalStreamQuery(dataSource, sql);
    }

    @Override
    public <T> T inTransaction(Function<JdbcClient, T> fn) {
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                T result = fn.apply(new TransactionalJdbcClient(conn));
                conn.commit();
                return result;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Transaction failed", e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to obtain connection", e);
        }
    }

    DataSource getDataSource() {
        return dataSource;
    }
}
