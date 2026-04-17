package dk.wandywharang.service;

import dk.wandywharang.api.Register;
import io.smallrye.mutiny.Uni;

public interface RegisterService {

    Uni<Void> register(Register register);
}
