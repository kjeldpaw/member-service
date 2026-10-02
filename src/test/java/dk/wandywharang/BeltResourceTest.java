package dk.wandywharang;

import dk.wandywharang.api.Belt;
import dk.wandywharang.service.BeltService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(BeltResource.class)
class BeltResourceTest {

    @InjectMock
    BeltService service;

    private static Belt belt() {
        return new Belt(UUID.randomUUID(), "Yellow Belt (9. KUP)", Duration.ZERO, 11);
    }

    @Test
    void findAllIsNotAllowedAnonymously() {
        given().get().then().statusCode(401);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findAllReturnsBelts() {
        final var belt = belt();
        when(service.findAll()).thenReturn(Uni.createFrom().item(List.of(belt)));

        given().get()
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo(belt.name()));
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findByIdReturnsBelt() {
        final var belt = belt();
        when(service.findById(belt.id())).thenReturn(Uni.createFrom().item(belt));

        given().get("/{id}", belt.id())
                .then().statusCode(200)
                .body("rank", equalTo(11));
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findByIdOfUnknownBeltIsNotFound() {
        final var id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Uni.createFrom().nullItem());

        given().get("/{id}", id).then().statusCode(404);
    }
}
