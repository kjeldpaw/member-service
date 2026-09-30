package dk.wandywharang.mapper;

import dk.wandywharang.api.AddMemberGraduationRequest;
import dk.wandywharang.api.Graduation;
import dk.wandywharang.api.UpdateMemberGraduationRequest;
import dk.wandywharang.entity.GraduationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi", uses = {BeltMapper.class, MemberMapper.class, OptionalMapper.class})
public interface GraduationMapper {

    @Mapping(target = "previousGraduation", expression = "java(entity.getPreviousGraduation().map(this::map))")
    Graduation map(GraduationEntity entity);

    // The belt, examiners and previous graduation are looked up by the service
    @Mapping(target = "graduatedBy", ignore = true)
    @Mapping(target = "belt", ignore = true)
    @Mapping(target = "previousGraduation", ignore = true)
    GraduationEntity map(AddMemberGraduationRequest request);

    @Mapping(target = "graduatedBy", ignore = true)
    @Mapping(target = "belt", ignore = true)
    @Mapping(target = "previousGraduation", ignore = true)
    GraduationEntity map(UpdateMemberGraduationRequest request, @MappingTarget GraduationEntity entity);
}
