package dk.wandywharang.service;

import dk.wandywharang.api.Belt;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.repository.BeltRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
@Transactional
public class BeltServiceImpl implements BeltService {
    private final BeltRepository repository;
    private final BeltMapper beltMapper;

    public BeltServiceImpl(BeltRepository beltRepository,
                           @SuppressWarnings("CdiInjectionPointsInspection") BeltMapper beltMapper) {
        this.repository = beltRepository;
        this.beltMapper = beltMapper;
    }

    @Override
    public Multi<? extends Belt> findAll() {
        return repository.findAll().list()
                .onItem().transformToMulti(belts -> Multi.createFrom().iterable(belts));
    }

    @Override
    public Uni<? extends Belt> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Uni<? extends Belt> create(Belt belt) {
        return repository.persist(beltMapper.toEntity(belt));
    }

    @Override
    public Uni<Void> delete(UUID id) {
        return repository.deleteById(id)
                .chain(v -> Uni.createFrom().voidItem());
    }

    @Override
    public Uni<Void> update(UUID id, Belt belt) {
        return repository.findById(id)
                .map(beltEntity -> beltMapper.updateEntity(belt, beltEntity))
                .chain(v -> Uni.createFrom().voidItem());
    }
}
