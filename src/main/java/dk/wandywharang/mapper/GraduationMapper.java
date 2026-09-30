package dk.wandywharang.mapper;

import dk.wandywharang.api.Graduation;
import dk.wandywharang.entity.GraduationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi", uses = {BeltMapper.class, MemberMapper.class, OptionalMapper.class})
public interface GraduationMapper {

    Graduation map(GraduationEntity entity);

    GraduationEntity map(Graduation data);

    GraduationEntity map(Graduation data, @MappingTarget GraduationEntity entity);


}
