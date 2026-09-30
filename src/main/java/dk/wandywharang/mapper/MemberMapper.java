package dk.wandywharang.mapper;

import dk.wandywharang.api.CreateMemberRequest;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.UpdateMemberRequest;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi", uses = {BeltMapper.class, AddressMapper.class, ReferenceMapper.class, GraduationMapper.class, OptionalMapper.class})
public interface MemberMapper {

    Member map(MemberEntity member);

    @Mapping(target = "phone", expression = "java(member.phone().orElse(null))")
    @Mapping(target = "dateOfBirth", expression = "java(member.dateOfBirth().orElse(null))")
    MemberEntity map(Member member);

    @Mapping(target = "phone", expression = "java(member.phone().orElse(null))")
    MemberEntity map(CreateMemberRequest request);

    @Mapping(target = "phone", expression = "java(request.phone().orElse(null))")
    MemberEntity map(UpdateMemberRequest request, @MappingTarget MemberEntity entity);

    default Set<Member> map(Set<MemberEntity> memberEntities) {
        return memberEntities.stream()
                .map(this::map)
                .collect(Collectors.toSet());
    }
 }
