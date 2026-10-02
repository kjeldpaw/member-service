package dk.wandywharang;

import dk.wandywharang.api.Address;
import dk.wandywharang.api.CreateGraduationRequest;
import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.CreateReferenceRequest;
import dk.wandywharang.api.ReferenceType;
import dk.wandywharang.api.UpdateGraduationRequest;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.api.UpdateReferenceRequest;
import dk.wandywharang.service.GraduationService;
import dk.wandywharang.service.MemberService;
import dk.wandywharang.service.ReferenceService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(MemberResource.class)
class MemberResourceTest {
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID BELT_ID = UUID.randomUUID();
    private static final UUID EXAMINER_ID = UUID.randomUUID();
    private static final UUID CHILD_ID = UUID.randomUUID();

    @InjectMock
    MemberService memberService;
    @InjectMock
    ReferenceService referenceService;
    @InjectMock
    GraduationService graduationService;

    private static Map<String, Object> memberBody() {
        return Map.of(
                "firstName", "Kim",
                "lastName", "Lee",
                "address", Map.of("street", "Main Street 1", "city", "Aarhus", "zipCode", "8000"),
                "email", "kim@example.com",
                "clubId", CLUB_ID);
    }

    private static Map<String, Object> graduationBody() {
        return Map.of("date", "2026-06-01", "graduatedBy", List.of(EXAMINER_ID), "beltId", BELT_ID);
    }

    @Test
    void findAllIsNotAllowedAnonymously() {
        given().get().then().statusCode(401);
        verifyNoInteractions(memberService);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findAllReturnsMembers() {
        final var member = TestData.member();
        when(memberService.findAll()).thenReturn(Uni.createFrom().item(List.of(member)));

        given().get()
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo(member.id().toString()))
                .body("[0].email", equalTo(member.email()));
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findByIdReturnsMember() {
        final var member = TestData.member();
        when(memberService.findById(member.id())).thenReturn(Uni.createFrom().item(member));

        given().get("/{id}", member.id())
                .then().statusCode(200)
                .body("firstName", equalTo("Kim"));
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void findByIdOfHiddenMemberIsNotFound() {
        when(memberService.findById(MEMBER_ID)).thenReturn(Uni.createFrom().failure(new NotFoundException()));

        given().get("/{id}", MEMBER_ID).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void createReturnsCreatedMember() {
        final var member = TestData.member();
        when(memberService.create(any())).thenReturn(Uni.createFrom().item(member));

        given().contentType("application/json").body(memberBody()).post()
                .then().statusCode(201)
                .body("id", equalTo(member.id().toString()));
        verify(memberService).create(CreateMemberRequest.builder()
                .firstName("Kim")
                .lastName("Lee")
                .address(new Address("Main Street 1", "Aarhus", "8000"))
                .phone(Optional.empty())
                .email("kim@example.com")
                .dateOfBirth(Optional.empty())
                .clubId(CLUB_ID)
                .build());
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void createInForbiddenClubIsForbidden() {
        when(memberService.create(any())).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        given().contentType("application/json").body(memberBody()).post()
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void createIsNotAllowedForMembers() {
        given().contentType("application/json").body(memberBody()).post()
                .then().statusCode(403);
        verifyNoInteractions(memberService);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void createWithInvalidEmailIsBadRequest() {
        final var body = new HashMap<>(memberBody());
        body.put("email", "not-an-email");

        given().contentType("application/json").body(body).post()
                .then().statusCode(400);
        verifyNoInteractions(memberService);
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void updateReturnsUpdatedMember() {
        final var member = TestData.member();
        when(memberService.update(eq(MEMBER_ID), any(UpdateMemberRequest.class))).thenReturn(Uni.createFrom().item(member));

        given().contentType("application/json").body(memberBody()).put("/{id}", MEMBER_ID)
                .then().statusCode(200)
                .body("id", equalTo(member.id().toString()));
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void updateOfMemberInOtherClubIsForbidden() {
        when(memberService.update(eq(MEMBER_ID), any(UpdateMemberRequest.class)))
                .thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        given().contentType("application/json").body(memberBody()).put("/{id}", MEMBER_ID)
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void createReferenceReturnsMember() {
        final var member = TestData.member();
        when(referenceService.create(MEMBER_ID, new CreateReferenceRequest(ReferenceType.KukkiWon, "KW-123")))
                .thenReturn(Uni.createFrom().item(member));

        given().contentType("application/json").body(Map.of("type", "KukkiWon", "reference", "KW-123"))
                .post("/{id}/reference", MEMBER_ID)
                .then().statusCode(201)
                .body("id", equalTo(member.id().toString()));
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void createReferenceWithoutReferenceIsBadRequest() {
        given().contentType("application/json").body(Map.of("type", "KukkiWon"))
                .post("/{id}/reference", MEMBER_ID)
                .then().statusCode(400);
        verifyNoInteractions(referenceService);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void createReferenceIsNotAllowedForMembers() {
        given().contentType("application/json").body(Map.of("type", "KukkiWon", "reference", "KW-123"))
                .post("/{id}/reference", MEMBER_ID)
                .then().statusCode(403);
        verifyNoInteractions(referenceService);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void updateReferenceReturnsMember() {
        when(referenceService.update(MEMBER_ID, CHILD_ID, new UpdateReferenceRequest("KW-456")))
                .thenReturn(Uni.createFrom().item(TestData.member()));

        given().contentType("application/json").body(Map.of("reference", "KW-456"))
                .put("/{id}/reference/{referenceId}", MEMBER_ID, CHILD_ID)
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void deleteOfUnknownReferenceIsNotFound() {
        when(referenceService.delete(MEMBER_ID, CHILD_ID)).thenReturn(Uni.createFrom().failure(new NotFoundException()));

        given().delete("/{id}/reference/{referenceId}", MEMBER_ID, CHILD_ID)
                .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void createGraduationReturnsMember() {
        final var request = new CreateGraduationRequest(LocalDate.of(2026, 6, 1), Set.of(EXAMINER_ID), BELT_ID);
        when(graduationService.create(MEMBER_ID, request)).thenReturn(Uni.createFrom().item(TestData.member()));

        given().contentType("application/json").body(graduationBody())
                .post("/{id}/graduation", MEMBER_ID)
                .then().statusCode(201);
        verify(graduationService).create(MEMBER_ID, request);
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void createGraduationWithoutBeltIsBadRequest() {
        given().contentType("application/json").body(Map.of("date", "2026-06-01", "graduatedBy", List.of()))
                .post("/{id}/graduation", MEMBER_ID)
                .then().statusCode(400);
        verifyNoInteractions(graduationService);
    }

    @Test
    @TestSecurity(user = "member", roles = "member")
    void createGraduationIsNotAllowedForMembers() {
        given().contentType("application/json").body(graduationBody())
                .post("/{id}/graduation", MEMBER_ID)
                .then().statusCode(403);
        verifyNoInteractions(graduationService);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void updateGraduationReturnsMember() {
        final var request = new UpdateGraduationRequest(LocalDate.of(2026, 6, 1), Set.of(EXAMINER_ID), BELT_ID);
        when(graduationService.update(MEMBER_ID, CHILD_ID, request)).thenReturn(Uni.createFrom().item(TestData.member()));

        given().contentType("application/json").body(graduationBody())
                .put("/{id}/graduation/{graduationId}", MEMBER_ID, CHILD_ID)
                .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "instructor", roles = "instructor")
    void deleteGraduationOfMemberInOtherClubIsForbidden() {
        when(graduationService.delete(MEMBER_ID, CHILD_ID)).thenReturn(Uni.createFrom().failure(new ForbiddenException()));

        given().delete("/{id}/graduation/{graduationId}", MEMBER_ID, CHILD_ID)
                .then().statusCode(403);
    }
}
