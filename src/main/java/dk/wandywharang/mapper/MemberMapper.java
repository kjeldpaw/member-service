package dk.wandywharang.mapper;

import dk.wandywharang.api.MemberRecord;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface MemberMapper {

    MemberRecord toRecord(MemberEntity member);
}
