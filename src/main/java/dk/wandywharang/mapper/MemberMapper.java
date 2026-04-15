package dk.wandywharang.mapper;

import dk.wandywharang.api.MemberRecord;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface MemberMapper {

    MemberRecord toRecord(MemberEntity member);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    MemberEntity toEntity(MemberRecord record);
}
