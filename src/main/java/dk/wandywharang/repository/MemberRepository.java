package dk.wandywharang.repository;

import dk.wandywharang.entity.GraduationEntity;
import dk.wandywharang.entity.MemberEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.UUID;

@ApplicationScoped
public class MemberRepository implements PanacheRepositoryBase<MemberEntity, UUID> {

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
