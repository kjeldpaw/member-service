package dk.wandywharang.service;

import lombok.Getter;

/**
 * A search query has too few characters to narrow the result down.
 */
@Getter
public class QueryTooShortException extends RuntimeException {
    private final int minimumLength;

    public QueryTooShortException(int minimumLength) {
        super(String.format("Query must be at least %d characters", minimumLength));
        this.minimumLength = minimumLength;
    }
}
