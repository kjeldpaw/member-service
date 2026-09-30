package dk.wandywharang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class EmbeddedAddress  {
    private String street;

    private String city;

    @Column(name = "zip_code")
    private String zipCode;

 }
