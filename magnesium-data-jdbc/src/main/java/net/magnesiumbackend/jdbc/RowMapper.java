package net.magnesiumbackend.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps a single row from a ResultSet to an object.
 *
 * @param <T> the type to map to
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the ResultSet to an object.
     *
     * @param rs the result set positioned at the current row
     * @param rowNum the row number (1-based)
     * @return the mapped object
     * @throws SQLException if a database error occurs
     */
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
