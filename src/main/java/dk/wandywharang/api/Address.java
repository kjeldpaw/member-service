package dk.wandywharang.api;

import java.util.Optional;

public interface Address {

    Optional<String> getStreet();

    Optional<String> getCity();

    Optional<String> getZipCode();

}
