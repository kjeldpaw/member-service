package dk.wandywharang.mapper;

import dk.wandywharang.api.CreateReferenceRequest;
import dk.wandywharang.api.Reference;
import dk.wandywharang.api.UpdateReferenceRequest;
import dk.wandywharang.entity.ReferenceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "cdi")
public interface ReferenceMapper {

    Reference map(ReferenceEntity entity);

    @Mapping(target = "member", ignore = true)
    ReferenceEntity map(Reference data);

    @Mapping(target = "member", ignore = true)
    ReferenceEntity map(Reference data, @MappingTarget ReferenceEntity entity);

    // The member is set by the service
    @Mapping(target = "member", ignore = true)
    ReferenceEntity map(CreateReferenceRequest request);

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "member", ignore = true)
    ReferenceEntity map(UpdateReferenceRequest request, @MappingTarget ReferenceEntity entity);
}
