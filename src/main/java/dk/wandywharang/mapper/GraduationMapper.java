package dk.wandywharang.mapper;

import dk.wandywharang.api.CreateGraduationRequest;
import dk.wandywharang.api.Examiner;
import dk.wandywharang.api.Graduation;
import dk.wandywharang.api.UpdateGraduationRequest;
import dk.wandywharang.entity.GraduationEntity;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi", uses = {BeltMapper.class, OptionalMapper.class})
public interface GraduationMapper {

    @Mapping(target = "previousGraduation", expression = "java(entity.getPreviousGraduation().map(this::map))")
    Graduation map(GraduationEntity entity);

    Examiner map(MemberEntity member);

    // The belt, examiners and previous graduation are looked up by the service
    @Mapping(target = "graduatedBy", ignore = true)
    @Mapping(target = "belt", ignore = true)
    @Mapping(target = "previousGraduation", ignore = true)
    GraduationEntity map(CreateGraduationRequest request);

    @Mapping(target = "graduatedBy", ignore = true)
    @Mapping(target = "belt", ignore = true)
    @Mapping(target = "previousGraduation", ignore = true)
    GraduationEntity map(UpdateGraduationRequest request, @MappingTarget GraduationEntity entity);
}
