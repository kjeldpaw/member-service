package dk.wandywharang.repository;

import dk.wandywharang.entity.MemberEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
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
     * Finds the Members of any Club whose first name, last name or full name contains the text, ignoring case,
     * ordered by last name and then first name, and at most the given number of them.
     */
    public Uni<List<MemberEntity>> searchByName(String text, int limit) {
        // Escape LIKE's wildcards, so they only match themselves
        final var escaped = text.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return find("lower(firstName) like ?1 escape '\\' or lower(lastName) like ?1 escape '\\'"
                        + " or lower(concat(firstName, ' ', lastName)) like ?1 escape '\\'",
                Sort.by("lastName").and("firstName").and("id"),
                "%" + escaped + "%")
                .range(0, limit - 1)
                .list();
    }

    /**
     * Deletes the Member with the records that depend on them: their References, their Graduation history, their
     * Club links and their place as examiner on other Members' Graduations. Those Graduations themselves are kept.
     */
    public Uni<Void> deleteWithDependents(MemberEntity member) {
        final var history = member.graduationHistory();
        Uni<?> deletes = getSession()
                .chain(session -> session.createMutationQuery("delete from ReferenceEntity r where r.member = :member")
                        .setParameter("member", member)
                        .executeUpdate())
                .chain(() -> getSession())
                .chain(session -> session.createNativeQuery("delete from graduation_members where member_id = :id")
                        .setParameter("id", member.getId())
                        .executeUpdate())
                .chain(() -> delete(member))
                .chain(this::flush);
        // Latest first, as each Graduation is referenced by the one after it
        for (final var graduation : history) {
            deletes = deletes.chain(() -> getSession()).chain(session -> session.remove(graduation))
                    .chain(this::flush);
        }
        return deletes.replaceWithVoid();
    }

    /**
     * Fetches the lazy associations needed to map a member: its references and the examiners of every graduation in
     * its graduation history. The fetches run one after another, as a reactive session does not allow concurrent use.
     */
    public Uni<MemberEntity> fetchDetails(MemberEntity member) {
        Uni<?> fetches = member.getReferences() == null
                ? Uni.createFrom().voidItem()
                : Mutiny.fetch(member.getReferences());
        for (final var graduation : member.graduationHistory()) {
            final var graduatedBy = graduation.getGraduatedBy();
            if (graduatedBy != null) {
                fetches = fetches.chain(() -> Mutiny.fetch(graduatedBy));
            }
        }
        return fetches.replaceWith(member);
    }
}
