package dk.wandywharang.it;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Runs the application under the "integration" config profile instead of "test", so none of the {@code %test.*}
 * overrides in application.properties apply. That leaves Dev Services and the OIDC tenant enabled, which starts real
 * Postgres and Keycloak containers (via Testcontainers) and runs Liquibase against them, instead of the mocks and
 * fixed datasource the unit tests use.
 */
public class IntegrationTestProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
        return "integration";
    }
}
