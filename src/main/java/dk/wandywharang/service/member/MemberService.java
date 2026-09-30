package dk.wandywharang.service.member;

import dk.wandywharang.api.Club;
import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface MemberService {

    Uni<List<Member>> findAll();

    Uni<Member> findById(UUID id);

    Uni<List<Member>> findByClub(Club club);

    Uni<Member> create(CreateMemberRequest request);

}
