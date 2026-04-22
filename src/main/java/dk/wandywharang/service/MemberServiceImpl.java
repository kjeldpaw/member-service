package dk.wandywharang.service;

import dk.wandywharang.api.Member;
import dk.wandywharang.mapper.MemberMapper;
import dk.wandywharang.repository.MemberRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MemberServiceImpl implements MemberService {
    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;

    public MemberServiceImpl(MemberMapper memberMapper, MemberRepository memberRepository) {
        this.memberMapper = memberMapper;
        this.memberRepository = memberRepository;
    }

    @Override
    public Uni<Void> apply(Member member) {
        return null;
    }

    @Override
    public Multi<? extends Member> findAll() {
        return null;
    }

    @Override
    public Multi<? extends Member> findByClub(String id) {
        return null;
    }
}
