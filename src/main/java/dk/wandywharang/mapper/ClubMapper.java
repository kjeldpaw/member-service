package dk.wandywharang.mapper;

import dk.wandywharang.api.Club;
import dk.wandywharang.entity.ClubEntity;
import org.mapstruct.Mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "cdi", uses = {AddressMapper.class})
public interface ClubMapper {

    Club map(ClubEntity entity);

    default List<Club> map(List<ClubEntity> entities) {
        if (entities != null) {
            return entities.stream()
                    .map(this::map)
                    .toList();
        } else {
            return List.of();
        }
    }

    /**
     * Maps a Member's Clubs, sorted by name so the order is stable.
     */
    default List<Club> map(Set<ClubEntity> entities) {
        if (entities != null) {
            return entities.stream()
                    .sorted(Comparator.comparing(ClubEntity::getName))
                    .map(this::map)
                    .toList();
        } else {
            return List.of();
        }
    }
}
