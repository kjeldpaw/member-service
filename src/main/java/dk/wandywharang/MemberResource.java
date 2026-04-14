package dk.wandywharang;

import dk.wandywharang.api.Member;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.MemberRepository;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/members")
public class MemberResource {
    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    public MemberResource(MemberRepository memberRepository,
                          MemberMapper memberMapper) {
        this.memberRepository = memberRepository;
        this.memberMapper = memberMapper;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<? extends Member> members() {
        return memberRepository.findAll().list()
                .onItem().transformToMulti(members -> Multi.createFrom().items(members.stream()))
                .onItem().transform(memberMapper::toRecord);
    }
}
