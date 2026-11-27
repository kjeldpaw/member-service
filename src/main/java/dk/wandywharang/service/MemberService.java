package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.api.UpdateProfileRequest;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface MemberService {

    Uni<List<Member>> findAll();

    Uni<Member> findById(UUID id);

    /**
     * The current user's own Member record, their Profile.
     */
    Uni<Member> findProfile();

    /**
     * Updates the current user's Profile. Every role may do this, but it never changes the email or the Clubs.
     */
    Uni<Member> updateProfile(UpdateProfileRequest request);

    Uni<Member> create(CreateMemberRequest request);

    Uni<Member> update(UUID id, UpdateMemberRequest request);

    /**
     * Adds the Member to the Club, unless they already belong to it.
     */
    Uni<Member> addClub(UUID id, UUID clubId);

    /**
     * Removes the Member from the Club, unless they don't belong to it.
     */
    Uni<Member> removeClub(UUID id, UUID clubId);

    /**
     * Deletes the Member and then their Keycloak user. A Keycloak user that is already gone is not an error.
     */
    Uni<Void> delete(UUID id);
}
