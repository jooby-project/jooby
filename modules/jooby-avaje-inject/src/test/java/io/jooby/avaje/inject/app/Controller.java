package io.jooby.avaje.inject.app;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.avaje.inject.InjectModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Path("")
@InjectModule(requires = {JsonMapper.class})
public class Controller {

    private final JsonMapper jsonMapper;

    @Inject
    public Controller(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @GET
    @Path("/ping")
    public String ping() {
        jsonMapper.version();
        return "pong";
    }
}
