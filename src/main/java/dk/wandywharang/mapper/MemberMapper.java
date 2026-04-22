package dk.wandywharang.mapper;
import dk.wandywharang.api.Graduation;
import dk.wandywharang.api.Member;
import dk.wandywharang.api.record.MemberRecord;
import dk.wandywharang.entity.GraduationEntity;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(componentModel = "cdi")
public interface MemberMapper {

    MemberRecord toRecord(MemberEntity member);

    MemberEntity toEntity(Member member);

    default Graduation map(Optional<? extends Graduation> value) {
        return value.orElse(null);
    }

    default GraduationEntity mapToEntity(Optional<? extends Graduation> value) {
        return (GraduationEntity) value.orElse(null);
    }
}
