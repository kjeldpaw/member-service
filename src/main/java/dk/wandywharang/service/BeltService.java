package dk.wandywharang.service;

import dk.wandywharang.api.Belt;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface BeltService {

    Uni<List<? extends Belt>> findAll();

    Uni<? extends Belt> findById(UUID id);

    Uni<? extends Belt> create(Belt belt);

    Uni<Void> delete(UUID id);

    Uni<Void> update(UUID id, Belt belt);
}
