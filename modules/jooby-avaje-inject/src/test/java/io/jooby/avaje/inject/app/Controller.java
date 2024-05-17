package io.jooby.avaje.inject.app;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.typesafe.config.Config;
import io.avaje.inject.InjectModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@Path("")
@InjectModule(requires = {JsonMapper.class, Config.class})
public class Controller {

    private final JsonMapper jsonMapper;
    private final Config config;

    @Inject
    public Controller(JsonMapper jsonMapper, Config config) {
        this.jsonMapper = jsonMapper;
        this.config = config;
    }

    @GET
    @Path("/ping")
    public String ping() {
        jsonMapper.version();
        config.isEmpty();

        return "pong";
    }
}
