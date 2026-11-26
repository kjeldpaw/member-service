package dk.wandywharang.service;

import dk.wandywharang.api.Examiner;
import dk.wandywharang.mapper.GraduationMapper;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
public class ExaminerServiceImpl implements ExaminerService {
    static final int MINIMUM_QUERY_LENGTH = 2;
    static final int MAXIMUM_RESULTS = 20;

    private final MemberRepository repository;
    private final GraduationMapper mapper;

    @Override
    @WithSession
    public Uni<List<Examiner>> search(String query) {
        final var text = query == null ? "" : query.strip();
        if (text.length() < MINIMUM_QUERY_LENGTH) {
            return Uni.createFrom().failure(new QueryTooShortException(MINIMUM_QUERY_LENGTH));
        }
        return repository.searchByName(text, MAXIMUM_RESULTS)
                .map(members -> members.stream().map(mapper::map).toList());
    }
}
