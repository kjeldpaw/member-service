package dk.wandywharang.api.record;

import dk.wandywharang.api.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

public record MemberRecord(String id, String firstName, String lastName, Address address, String phone, String email, LocalDate dateOfBirth, Club club, Graduation graduation, Set<Reference> references) implements Member {

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    @Override
    public Club getClub() {
        return club;
    }

    @Override
    public Optional<? extends Graduation> getGraduation() {
        return Optional.ofNullable(graduation);
    }

    @Override
    public Set<? extends Reference> getReferences() {
        return references != null ? references : Set.of();
    }
}
