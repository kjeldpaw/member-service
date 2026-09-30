package dk.wandywharang.service;

import dk.wandywharang.api.CreateReferenceRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateReferenceRequest;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface ReferenceService {

    Uni<Member> create(UUID memberId, CreateReferenceRequest request);

    Uni<Member> update(UUID memberId, UUID referenceId, UpdateReferenceRequest reference);

    Uni<Member> delete(UUID memberId, UUID referenceId);

}
