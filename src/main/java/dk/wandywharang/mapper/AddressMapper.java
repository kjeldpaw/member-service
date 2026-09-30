package dk.wandywharang.mapper;

import dk.wandywharang.api.Address;
import dk.wandywharang.entity.EmbeddedAddress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface AddressMapper {

    Address map(EmbeddedAddress address);

    EmbeddedAddress map(Address address);

}
