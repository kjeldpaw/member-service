package dk.wandywharang.service;

import dk.wandywharang.TestData;
import dk.wandywharang.api.Belt;
import dk.wandywharang.entity.BeltEntity;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.repository.BeltRepository;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeltServiceImplTest {
    @Mock
    BeltRepository repository;
    @Mock
    BeltMapper mapper;

    BeltServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BeltServiceImpl(repository, mapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllReturnsAllBelts() {
        final var entities = List.of(TestData.belt());
        final var belts = List.of(new Belt(UUID.randomUUID(), "Yellow Belt (9. KUP)", Duration.ZERO, 11));
        final PanacheQuery<BeltEntity> query = mock(PanacheQuery.class);
        when(query.list()).thenReturn(Uni.createFrom().item(entities));
        when(repository.findAll()).thenReturn(query);
        when(mapper.map(entities)).thenReturn(belts);

        assertEquals(belts, service.findAll().await().indefinitely());
    }

    @Test
    void findByIdReturnsBelt() {
        final var entity = TestData.belt();
        final var belt = new Belt(entity.getId(), "Yellow Belt (9. KUP)", Duration.ZERO, 11);
        when(repository.findById(entity.getId())).thenReturn(Uni.createFrom().item(entity));
        when(mapper.map(entity)).thenReturn(belt);

        assertEquals(belt, service.findById(entity.getId()).await().indefinitely());
    }

    @Test
    void findByIdOfUnknownBeltReturnsNull() {
        final var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Uni.createFrom().nullItem());

        assertNull(service.findById(id).await().indefinitely());
    }
}
