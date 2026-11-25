package dk.wandywharang;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Logs and telemetry report quarkus.application.version, so it must be the Gradle project version a release is built
 * with (ADR 0001). The test task passes that version in as the gradle.project.version system property.
 */
@QuarkusTest
class ApplicationVersionTest {

    @ConfigProperty(name = "quarkus.application.version")
    String applicationVersion;

    @Test
    void applicationVersionIsTheGradleProjectVersion() {
        String gradleVersion = System.getProperty("gradle.project.version");
        assertNotNull(gradleVersion, "the test task must set the gradle.project.version system property");
        assertEquals(gradleVersion, applicationVersion);
    }
}
