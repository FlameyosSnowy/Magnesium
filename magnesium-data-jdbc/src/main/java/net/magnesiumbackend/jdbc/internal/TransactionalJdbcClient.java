package net.magnesiumbackend.jdbc.internal;

import net.magnesiumbackend.jdbc.JdbcClient;
import net.magnesiumbackend.jdbc.JdbcQuery;
import net.magnesiumbackend.jdbc.Update;
import net.magnesiumbackend.jdbc.streaming.StreamQuery;

import java.sql.Connection;
import java.util.function.Function;

/**
 * JdbcClient implementation for use within a transaction.
 *
 * <p>Uses the same Connection for all operations.</p>
 */
class TransactionalJdbcClient implements JdbcClient {

    private final Connection connection;

    TransactionalJdbcClient(Connection connection) {
        this.connection = connection;
    }

    @Override
    public JdbcQuery query(String sql) {
        return new TransactionalJdbcQuery(connection, sql);
    }

    @Override
    public Update update(String sql) {
        return new TransactionalUpdate(connection, sql);
    }

    @Override
    public StreamQuery stream(String sql) {
        return new TransactionalStreamQuery(connection, sql);
    }

    @Override
    public <T> T inTransaction(Function<JdbcClient, T> fn) {
        throw new IllegalStateException("Nested transactions not supported");
    }
}
