/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.langchain4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.ServiceRegistry;
import io.jooby.internal.langchain4j.FallbackChatModel;

class LangChain4jModuleTest {

  @Test
  @DisplayName("Should parse config and register custom factory models")
  void customFactoryRegistration() {
    var app =
        createApp(
            ConfigFactory.parseMap(
                Map.of(
                    "langchain4j.models.my-agent.provider", "custom",
                    "langchain4j.models.my-agent.api-key", "secret")));

    ChatModelFactory mockFactory = mock(ChatModelFactory.class);
    ChatModel mockChatModel = mock(ChatModel.class);
    StreamingChatModel mockStreamModel = mock(StreamingChatModel.class);

    when(mockFactory.createChatModel(any())).thenReturn(mockChatModel);
    when(mockFactory.createStreamingModel(any())).thenReturn(mockStreamModel);

    LangChain4jModule module = new LangChain4jModule().register("custom", mockFactory);

    module.install(app);

    ServiceRegistry services = app.getServices();

    // Check specific named registration
    assertEquals(mockChatModel, services.get(ChatModel.class));
    assertEquals(mockStreamModel, services.get(StreamingChatModel.class));

    // Check default (unnamed) registration since it's the first/only model
    assertEquals(mockChatModel, services.get(ChatModel.class));
    assertEquals(mockStreamModel, services.get(StreamingChatModel.class));
  }

  @NonNull private static Jooby createApp(Config config) {
    var app = new Jooby();
    var environment = mock(Environment.class);
    when(environment.getConfig()).thenReturn(config);
    app.setEnvironment(environment);
    return app;
  }

  @Test
  @DisplayName("Should wrap models in Fallback decorators when configured")
  void fallbackChainingRegistration() {
    Jooby app =
        createApp(
            ConfigFactory.parseMap(
                Map.of(
                    "langchain4j.models.primary.provider", "custom",
                    "langchain4j.models.primary.fallback", java.util.List.of("secondary"),
                    "langchain4j.models.secondary.provider", "custom")));

    ChatModelFactory mockFactory = mock(ChatModelFactory.class);
    when(mockFactory.createChatModel(any())).thenReturn(mock(ChatModel.class));

    LangChain4jModule module = new LangChain4jModule().register("custom", mockFactory);
    module.install(app);

    ServiceRegistry services = app.getServices();
    ChatModel primaryModel = services.get(ServiceKey.key(ChatModel.class, "primary"));
    ChatModel secondaryModel = services.get(ServiceKey.key(ChatModel.class, "secondary"));

    // The secondary should be standard, but the primary should be wrapped in the decorator
    assertFalse(secondaryModel instanceof FallbackChatModel);
    assertInstanceOf(FallbackChatModel.class, primaryModel);
  }

  @Test
  @DisplayName("Should fail fast if langchain4j.models configuration is missing")
  void missingConfigurationFailsFast() {
    Jooby app = createApp(ConfigFactory.empty());

    LangChain4jModule module = new LangChain4jModule();

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              module.install(app);
            });

    assertTrue(exception.getMessage().contains("no models found in configuration"));
  }
}
