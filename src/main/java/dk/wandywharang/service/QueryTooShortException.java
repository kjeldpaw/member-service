package dk.wandywharang.service;

/**
 * A search query has too few characters to narrow the result down.
 */
public class QueryTooShortException extends RuntimeException {

    public QueryTooShortException(String name, int minimumLength) {
        super(String.format("Query %s must be at least %d characters", name, minimumLength));
    }
}
