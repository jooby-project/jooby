package io.jooby.avaje.inject.app;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.typesafe.config.Config;
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
            Config config = require(Config.class);
            jsonMapper.version();
            config.isEmpty();
        });
    }
}
