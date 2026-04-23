package dk.wandywharang.mapper;

import dk.wandywharang.api.Club;
import dk.wandywharang.api.record.AddressRecord;
import dk.wandywharang.api.record.ClubRecord;
import dk.wandywharang.entity.Address;
import dk.wandywharang.entity.ClubEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.Optional;

@Mapper(componentModel = "cdi")
public interface ClubMapper {

    ClubRecord toRecord(Club club);

    ClubEntity toEntity(Club club);

    ClubEntity updateEntity(Club club, @MappingTarget ClubEntity entity);

    AddressRecord toRecord(dk.wandywharang.api.Address address);

    Address toEntity(dk.wandywharang.api.Address address);

    void updateEntity(dk.wandywharang.api.Address address, @MappingTarget Address entity);

    default String map(Optional<String> value) {
        return value.orElse(null);
    }
}
