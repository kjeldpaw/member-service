package dk.wandywharang;

import dk.wandywharang.api.record.RegisterRecord;
import dk.wandywharang.service.RegisterService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

@Path("/api/v1/register")
@RequestScoped
public class RegisterResource {
    private final RegisterService registerService;

    public RegisterResource(RegisterService registerService) {
        this.registerService = registerService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    public Uni<Void> register(RegisterRecord register) {
        return registerService.register(register);
    }
}
