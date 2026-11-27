package dk.wandywharang.mapper;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.api.UpdateProfileRequest;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi", uses = {AddressMapper.class, ClubMapper.class, ReferenceMapper.class, GraduationMapper.class, OptionalMapper.class})
public interface MemberMapper {

    Member map(MemberEntity member);

    // The id comes from Keycloak and the relations are resolved by the service
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clubs", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "phone", expression = "java(request.phone().orElse(null))")
    @Mapping(target = "dateOfBirth", expression = "java(request.dateOfBirth().orElse(null))")
    MemberEntity map(CreateMemberRequest request);

    // The email is the Keycloak username, so an update doesn't change it.
    // Updating a Member doesn't change their Clubs.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "clubs", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "phone", expression = "java(request.phone().orElse(null))")
    @Mapping(target = "dateOfBirth", expression = "java(request.dateOfBirth().orElse(null))")
    MemberEntity map(UpdateMemberRequest request, @MappingTarget MemberEntity entity);

    // A person cannot change their own email, which is their Keycloak login, or their own Clubs.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "clubs", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "phone", expression = "java(request.phone().orElse(null))")
    @Mapping(target = "dateOfBirth", expression = "java(request.dateOfBirth().orElse(null))")
    MemberEntity map(UpdateProfileRequest request, @MappingTarget MemberEntity entity);
}
