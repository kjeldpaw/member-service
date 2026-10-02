package dk.wandywharang.mapper;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi", uses = {AddressMapper.class, ClubMapper.class, ReferenceMapper.class, GraduationMapper.class, OptionalMapper.class})
public interface MemberMapper {

    Member map(MemberEntity member);

    // The id comes from Keycloak and the relations are resolved by the service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "phone", expression = "java(request.phone().orElse(null))")
    @Mapping(target = "dateOfBirth", expression = "java(request.dateOfBirth().orElse(null))")
    MemberEntity map(CreateMemberRequest request);

    // The email is the Keycloak username, so it is not changed by an update
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "club", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "phone", expression = "java(request.phone().orElse(null))")
    @Mapping(target = "dateOfBirth", expression = "java(request.dateOfBirth().orElse(null))")
    MemberEntity map(UpdateMemberRequest request, @MappingTarget MemberEntity entity);
}
