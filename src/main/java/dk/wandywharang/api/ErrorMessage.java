package dk.wandywharang.api;

/**
 * The body of an error response, for errors the client is expected to act on.
 */
public record ErrorMessage(String message) {
}
