package dk.wandywharang;

import dk.wandywharang.api.Belt;
import dk.wandywharang.api.EligibleMember;
import dk.wandywharang.service.EligibleService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(EligibleResource.class)
class EligibleResourceTest {

    @InjectMock
    EligibleService service;

    @Test
    void anonymousCallerIsUnauthorised() {
        given().queryParam("date", "2027-03-15").get().then().statusCode(401);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void plainMemberIsForbidden() {
        given().queryParam("date", "2027-03-15").get().then().statusCode(403);
        verifyNoInteractions(service);
    }

    @Test
    @TestSecurity(user = "instructor", roles = {"member", "instructor"})
    void instructorGetsTheEligibleMembersOnTheDate() {
        final var dan1 = new Belt(UUID.randomUUID(), "Black Belt (1. DAN)", Duration.ofDays(365), 24);
        final var dan2 = new Belt(UUID.randomUUID(), "Black Belt (2. DAN)", Duration.ofDays(730), 25);
        final var eligible = EligibleMember.builder()
                .id(UUID.randomUUID()).firstName("Kim").lastName("Lee").clubs(List.of())
                .belt(dan1).graduationDate(LocalDate.of(2026, 4, 15))
                .nextBelt(dan2).eligibleSince(LocalDate.of(2027, 4, 15))
                .build();
        when(service.findEligible(LocalDate.of(2027, 3, 15))).thenReturn(Uni.createFrom().item(List.of(eligible)));

        given().queryParam("date", "2027-03-15").get()
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].nextBelt.name", equalTo("Black Belt (2. DAN)"))
                .body("[0].graduationDate", equalTo("2026-04-15"))
                .body("[0].eligibleSince", equalTo("2027-04-15"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"member", "admin"})
    void missingOrInvalidDateIsABadRequest() {
        given().get()
                .then().statusCode(400)
                .contentType("application/json")
                .body("message", equalTo("Query date must be a date as YYYY-MM-DD"));

        for (final var date : new String[]{"", "15-03-2027", "2027-02-30", "tomorrow"}) {
            given().queryParam("date", date).get().then().statusCode(400);
        }
        verifyNoInteractions(service);
    }
}
