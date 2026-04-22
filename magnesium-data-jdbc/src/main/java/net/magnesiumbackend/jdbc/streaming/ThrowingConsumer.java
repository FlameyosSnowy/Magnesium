package net.magnesiumbackend.jdbc.streaming;

/**
 * Consumer that can throw checked exceptions.
 *
 * @param <T> the consumed type
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception if an error occurs
     */
    void accept(T t) throws Exception;
}
