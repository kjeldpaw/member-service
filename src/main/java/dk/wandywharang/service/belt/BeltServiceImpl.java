package dk.wandywharang.service.belt;

import dk.wandywharang.api.Belt;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.repository.BeltRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BeltServiceImpl implements BeltService {
    private final BeltRepository repository;
    private final BeltMapper mapper;

    public BeltServiceImpl(BeltRepository repository,
                           BeltMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @WithSession
    public Uni<List<? extends Belt>> findAll() {
        return repository.findAll().list()
                .map(list -> list);
    }

    @Override
    @WithSession
    public Uni<? extends Belt> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    @WithTransaction
    public Uni<? extends Belt> create(Belt belt) {
        return repository.persist(mapper.toEntity(belt));
    }

    @Override
    @WithTransaction
    public Uni<Void> delete(UUID id) {
        return repository.deleteById(id)
                .chain(_ -> Uni.createFrom().voidItem());
    }

    @Override
    @WithTransaction
    public Uni<Void> update(UUID id, Belt belt) {
        return repository.findById(id)
                .map(entity -> mapper.updateEntity(belt, entity))
                .chain(_ -> Uni.createFrom().voidItem());
    }
}
