package net.magnesiumbackend.jdbc.streaming;

import java.util.Iterator;

/**
 * Iterator that can be closed to release resources.
 *
 * <p>Ensures:
 * <ul>
 *   <li>Connection closed</li>
 *   <li>ResultSet closed</li>
 *   <li>No leaks</li>
 * </ul></p>
 *
 * @param <T> the element type
 */
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {

    @Override
    boolean hasNext();

    @Override
    T next();

    /**
     * Closes the iterator and releases all resources.
     */
    @Override
    void close();
}
