package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.Club;
import dk.wandywharang.entity.ClubEntity;
import dk.wandywharang.mapper.ClubMapper;
import dk.wandywharang.repository.ClubRepository;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceImplTest {
    @Mock
    ClubRepository repository;
    @Mock
    ClubMapper mapper;

    ClubServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClubServiceImpl(repository, mapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllReturnsAllClubs() {
        final var entities = List.of(TestData.club());
        final var clubs = List.of(new Club(UUID.randomUUID(), "Taekwondo Club", null));
        final PanacheQuery<ClubEntity> query = mock(PanacheQuery.class);
        when(query.list()).thenReturn(Uni.createFrom().item(entities));
        when(repository.findAll()).thenReturn(query);
        when(mapper.map(entities)).thenReturn(clubs);

        assertEquals(clubs, service.findAll().await().indefinitely());
    }

    @Test
    void findByIdReturnsClub() {
        final var entity = TestData.club();
        final var club = new Club(entity.getId(), "Taekwondo Club", null);
        when(repository.findById(entity.getId())).thenReturn(Uni.createFrom().item(entity));
        when(mapper.map(entity)).thenReturn(club);

        assertEquals(club, service.findById(entity.getId()).await().indefinitely());
    }

    @Test
    void findByIdOfUnknownClubReturnsNull() {
        final var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Uni.createFrom().nullItem());

        assertNull(service.findById(id).await().indefinitely());
    }
}
