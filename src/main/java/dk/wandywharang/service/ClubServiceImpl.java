package dk.wandywharang.service;

import dk.wandywharang.api.Club;
import dk.wandywharang.mapper.ClubMapper;
import dk.wandywharang.repository.ClubRepository;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
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
    public Uni<List<Club>> findAll() {
        return repository.findAll().list()
                .map(mapper::map);
    }

    @Override
    @WithSession
    public Uni<Club> findById(UUID id) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(mapper::map);
    }

}
