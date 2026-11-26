package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.Examiner;
import dk.wandywharang.mapper.GraduationMapper;
import dk.wandywharang.repository.MemberRepository;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExaminerServiceImplTest {
    @Mock
    MemberRepository repository;
    @Mock
    GraduationMapper mapper;

    ExaminerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExaminerServiceImpl(repository, mapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "T", " T  ", "   "})
    void queryShorterThanTwoCharactersAfterStrippingIsTooShort(String query) {
        final var failure = assertThrows(QueryTooShortException.class,
                () -> service.search(query).await().indefinitely());

        assertEquals(2, failure.getMinimumLength());
        assertEquals("Query must be at least 2 characters", failure.getMessage());
        verify(repository, never()).searchByName(anyString(), anyInt());
    }

    @Test
    void searchesForTheStrippedQueryAndReturnsAtMostTwentyExaminers() {
        final var entity = TestData.member(TestData.club());
        final var examiner = new Examiner(entity.getId(), "Hana", "Kim");
        when(repository.searchByName("Hana Kim", 20)).thenReturn(Uni.createFrom().item(List.of(entity)));
        when(mapper.map(entity)).thenReturn(examiner);

        assertEquals(List.of(examiner), service.search("  Hana Kim \t").await().indefinitely());
    }
}
