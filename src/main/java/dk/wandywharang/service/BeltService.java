package dk.wandywharang.service;

import dk.wandywharang.api.Belt;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface BeltService {

    Uni<List<Belt>> findAll();

    Uni<Belt> findById(UUID id);

}
