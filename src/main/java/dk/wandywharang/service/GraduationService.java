package dk.wandywharang.service;

import dk.wandywharang.api.*;
import io.smallrye.mutiny.Uni;

import java.util.UUID;

public interface GraduationService {

    Uni<Member> create(UUID memberId, CreateGraduationRequest request);

    Uni<Member> update(UUID memberId, UUID graduationId, UpdateGraduationRequest graduation);

    Uni<Member> delete(UUID memberId, UUID graduationId);
}
