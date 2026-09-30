package dk.wandywharang.service;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface MemberService {

    Uni<List<Member>> findAll();

    Uni<Member> findById(UUID id);

    Uni<Member> create(CreateMemberRequest request);

    Uni<Member> update(UUID id, UpdateMemberRequest request);
}
