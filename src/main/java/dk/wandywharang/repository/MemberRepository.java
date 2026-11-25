package dk.wandywharang.repository;

import dk.wandywharang.entity.GraduationEntity;
import dk.wandywharang.entity.MemberEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MemberRepository implements PanacheRepositoryBase<MemberEntity, UUID> {

    /**
     * Finds the Members who share at least one Club with the given Member, including that Member.
     */
    public Uni<List<MemberEntity>> findSharingClubWith(MemberEntity member) {
        final var clubIds = member.clubIds();
        if (clubIds.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }
        return find("select distinct m from MemberEntity m join m.clubs c where c.id in ?1", clubIds).list();
    }

    /**
     * Fetches the lazy associations needed to map a member: its references and the examiners of every graduation in
     * its graduation history. The fetches run one after another, as a reactive session does not allow concurrent use.
     */
    public Uni<MemberEntity> fetchDetails(MemberEntity member) {
        Uni<?> fetches = member.getReferences() == null
                ? Uni.createFrom().voidItem()
                : Mutiny.fetch(member.getReferences());
        for (GraduationEntity graduation = member.getGraduation(); graduation != null;
             graduation = graduation.getPreviousGraduation().orElse(null)) {
            final var graduatedBy = graduation.getGraduatedBy();
            if (graduatedBy != null) {
                fetches = fetches.chain(() -> Mutiny.fetch(graduatedBy));
            }
        }
        return fetches.replaceWith(member);
    }
}
