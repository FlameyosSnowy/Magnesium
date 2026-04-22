package net.magnesiumbackend.jdbc;

/**
 * Update API for INSERT, UPDATE, DELETE operations.
 */
public interface Update {

    /**
     * Binds a value to a named parameter.
     *
     * @param name the parameter name
     * @param value the value to bind
     * @return this update for chaining
     */
    Update bind(String name, Object value);

    /**
     * Binds a value to a positional parameter (1-indexed).
     *
     * @param index the parameter index (1-based)
     * @param value the value to bind
     * @return this update for chaining
     */
    Update bind(int index, Object value);

    /**
     * Executes the update and returns affected row count.
     *
     * @return number of rows affected
     */
    int execute();

    /**
     * Executes the update with return of generated keys.
     *
     * @param keyMapper the mapper for the generated key
     * @param <T> the key type
     * @return the generated key
     */
    <T> T executeAndReturnKey(RowMapper<T> keyMapper);
}
