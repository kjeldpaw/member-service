package dk.wandywharang.mapper;

import dk.wandywharang.api.Member;
import dk.wandywharang.api.record.MemberRecord;
import dk.wandywharang.entity.MemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface MemberMapper {

    MemberRecord toRecord(MemberEntity member);

    MemberEntity toEntity(Member member);
}
