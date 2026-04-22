package dk.wandywharang.service;

import dk.wandywharang.api.Member;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface MemberService {

    Multi<? extends Member> findAll();

    Multi<? extends Member> findByClub(String id);

    Uni<Void> apply(Member member);

}
