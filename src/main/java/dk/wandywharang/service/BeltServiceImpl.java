package dk.wandywharang.service;

import dk.wandywharang.api.Belt;
import dk.wandywharang.mapper.BeltMapper;
import dk.wandywharang.repository.BeltRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
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
    public Uni<List<Belt>> findAll() {
        return repository.findAll().list()
                .map(mapper::map);
    }

    @Override
    @WithSession
    public Uni<Belt> findById(UUID id) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(mapper::map);
    }
}
