package io.jooby.avaje.inject.app;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.typesafe.config.Config;
import io.avaje.inject.InjectModule;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@Path("")
@InjectModule(requires = {JsonMapper.class, Config.class, String.class})
public class Controller {

    private final JsonMapper jsonMapper;
    private final Config config;
    private final String env;

    @Inject
    public Controller(JsonMapper jsonMapper, Config config, @Named("application.env") String env) {
        this.jsonMapper = jsonMapper;
        this.config = config;
        this.env = env;
    }

    @GET
    @Path("/ping")
    public String ping() {
        jsonMapper.version();
        config.isEmpty();

        return this.env;
    }
}
