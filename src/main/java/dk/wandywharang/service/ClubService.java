package dk.wandywharang.service;

import dk.wandywharang.api.Club;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface ClubService {

    Uni<List<Club>> findAll();

    Uni<Club> findById(UUID id);

}
