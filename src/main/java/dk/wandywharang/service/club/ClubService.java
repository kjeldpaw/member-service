package dk.wandywharang.service.club;

import dk.wandywharang.api.Club;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.UUID;

public interface ClubService {

    Uni<List<? extends Club>> findAll();

    Uni<? extends Club> findById(UUID id);

    Uni<? extends Club> create(Club club);

    Uni<Void> delete(UUID id);

    Uni<Void> update(UUID id, Club club);

}
