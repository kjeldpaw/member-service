package dk.wandywharang.mapper;

import dk.wandywharang.api.Belt;
import dk.wandywharang.entity.BeltEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface BeltMapper {

    Belt map(BeltEntity entity);

    default List<Belt> map(List<BeltEntity> entities) {
        if (entities != null) {
            return entities.stream()
                    .map(this::map)
                    .toList();
        } else {
            return List.of();
        }
    }

}
