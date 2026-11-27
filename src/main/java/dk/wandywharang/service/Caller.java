package dk.wandywharang.service;

import java.util.UUID;

/**
 * The logged-in person, as their token identifies them: their Keycloak user id and their email.
 */
public record Caller(UUID id, String email) {
}
