package dk.wandywharang.mapper;

import dk.wandywharang.api.Club;
import dk.wandywharang.entity.ClubEntity;
import org.mapstruct.Mapper;

import java.util.List;

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
}
