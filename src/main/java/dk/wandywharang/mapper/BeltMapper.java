package dk.wandywharang.mapper;

import dk.wandywharang.api.Belt;
import dk.wandywharang.api.record.BeltRecord;
import dk.wandywharang.entity.BeltEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface BeltMapper {

    BeltRecord toRecord(Belt belt);

    BeltEntity toEntity(Belt belt);

    BeltEntity updateEntity(Belt belt, @MappingTarget BeltEntity entity);

}
