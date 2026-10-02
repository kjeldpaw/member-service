package dk.wandywharang;

import dk.wandywharang.api.Address;
import dk.wandywharang.api.Club;
import dk.wandywharang.service.ClubService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(ClubResource.class)
class ClubResourceTest {

    @InjectMock
    ClubService service;

    private static Club club() {
        return new Club(UUID.randomUUID(), "Taekwondo Club", new Address("Main Street 1", "Aarhus", "8000"));
    }

    @Test
    void findAllIsNotAllowedAnonymously() {
        given().get().then().statusCode(401);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findAllReturnsClubs() {
        final var club = club();
        when(service.findAll()).thenReturn(Uni.createFrom().item(List.of(club)));

        given().get()
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].name", equalTo(club.name()));
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findByIdReturnsClub() {
        final var club = club();
        when(service.findById(club.id())).thenReturn(Uni.createFrom().item(club));

        given().get("/{id}", club.id())
                .then().statusCode(200)
                .body("address.city", equalTo("Aarhus"));
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findByIdOfUnknownClubIsNotFound() {
        final var id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Uni.createFrom().nullItem());

        given().get("/{id}", id).then().statusCode(404);
    }
}
