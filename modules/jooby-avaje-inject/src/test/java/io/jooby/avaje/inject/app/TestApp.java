package io.jooby.avaje.inject.app;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.jooby.Jooby;
import io.jooby.avaje.inject.AvajeInjectModule;
import io.jooby.jackson.JacksonModule;

public class TestApp extends Jooby {

    {
        install(new JacksonModule());
        install(AvajeInjectModule.of());

        mvc(Controller.class);

        onStarted(() -> {
            JsonMapper jsonMapper = require(JsonMapper.class);
            jsonMapper.version();
        });
    }
}
