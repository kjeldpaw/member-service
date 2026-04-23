package dk.wandywharang.service.club;

import dk.wandywharang.api.Club;
import dk.wandywharang.mapper.ClubMapper;
import dk.wandywharang.repository.ClubRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ClubServiceImpl implements ClubService {
    private final ClubRepository repository;
    private final ClubMapper mapper;

    public ClubServiceImpl(ClubRepository repository,
                           ClubMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @WithSession
    public Uni<List<? extends Club>> findAll() {
        return repository.findAll().list()
                .map(list -> list);
    }

    @Override
    @WithSession
    public Uni<? extends Club> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @WithTransaction
    public Uni<? extends Club> create(Club club) {
        return repository.persist(mapper.toEntity(club));
    }

    @Override
    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return repository.deleteById(id)
                .chain(_ -> Uni.createFrom().voidItem());
    }

    @Override
    @WithTransaction
    public Uni<Void> update(UUID id, Club club) {
        return repository.findById(id)
                .map(entity -> mapper.updateEntity(club, entity))
                .chain(_ -> Uni.createFrom().voidItem());
    }

}
