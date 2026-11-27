package dk.wandywharang.mapper;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.CreateProfileRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.api.UpdateProfileRequest;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Mapper(componentModel = "cdi", uses = {AddressMapper.class, ClubMapper.class, ReferenceMapper.class, GraduationMapper.class, OptionalMapper.class})
public interface MemberMapper {

    Member map(MemberEntity member);

    @FromRequest
    MemberEntity map(CreateMemberRequest request);

    // The email is the Keycloak username, so an update doesn't change it.
    // Updating a Member doesn't change their Clubs.
    @FromRequest
    @Mapping(target = "email", ignore = true)
    MemberEntity map(UpdateMemberRequest request, @MappingTarget MemberEntity entity);

    // The email comes from the logged-in person's token
    @FromRequest
    MemberEntity map(CreateProfileRequest request, String email);

    // A person cannot change their own email, which is their Keycloak login, or their own Clubs.
    @FromRequest
    @Mapping(target = "email", ignore = true)
    MemberEntity map(UpdateProfileRequest request, @MappingTarget MemberEntity entity);

    /**
     * A request never sets the id, which comes from Keycloak, or the relations, which the service resolves.
     */
    @Retention(RetentionPolicy.CLASS)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clubs", ignore = true)
    @Mapping(target = "graduation", ignore = true)
    @Mapping(target = "references", ignore = true)
    @interface FromRequest {
    }
}
