package dk.wandywharang.mapper;

import dk.wandywharang.api.Graduation;
import org.mapstruct.Mapper;

import java.time.LocalDate;
import java.util.Optional;

@Mapper(componentModel = "cdi")
public interface OptionalMapper {

    default Optional<String> map(String value) {
        return Optional.ofNullable(value);
    }

    default Optional<LocalDate> map(LocalDate value) {
        return Optional.ofNullable(value);
    }

    default Optional<Graduation> map(Graduation value) {
        return Optional.ofNullable(value);
    }
}
