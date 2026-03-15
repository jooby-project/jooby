/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.langchain4j;

import java.util.*;

import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.internal.langchain4j.BuiltInModel;
import io.jooby.internal.langchain4j.FallbackChatModel;
import io.jooby.internal.langchain4j.FallbackStreamingChatModel;

/**
 * Jooby Extension for LangChain4j.
 *
 * <p>This module provides seamless integration between Jooby and the LangChain4j 1.x ecosystem,
 * automatically instantiating and registering {@link dev.langchain4j.model.chat.ChatModel} and
 * {@link dev.langchain4j.model.chat.StreamingChatModel} components based on your application
 * configuration.
 *
 * <h3>Installation</h3>
 *
 * <p>Install the module inside your Jooby application:
 *
 * <pre>{@code
 * {
 *   install(new LangChain4jModule());
 *   get("/chat", ctx -> {
 *     ChatModel ai = require(ChatModel.class);
 *     return ai.chat("Hello world!");
 *   });
 *   }
 * }</pre>
 *
 * <h3>Configuration</h3>
 *
 * <p>Models are defined in your {@code application.conf} under the {@code langchain4j.models} key.
 * The module automatically creates both blocking and streaming interfaces if the provider supports
 * them.
 *
 * <pre>{@code
 * langchain4j {
 *   models {
 *   # The name of the registered service will be "gpt-assistant"
 *     gpt-assistant {
 *       provider = "openai"
 *       api-key = ${OPENAI_API_KEY}
 *       model-name = "gpt-4o-mini"
 *       timeout = 30s
 *       temperature = 0.7
 *    }
 *   }
 * }
 * }</pre>
 *
 * <h3>Resilience & Fallback Routing</h3>
 *
 * <p>You can define a chain of fallbacks to ensure high availability. If the primary model fails
 * (e.g., due to rate limits or network timeouts), the module automatically and silently routes the
 * request to the next configured fallback.
 *
 * <pre>{@code
 * langchain4j.models {
 *   primary-agent {
 *     provider = "openai"
 *     api-key = "..."
 *     fallback = ["local-failover"]
 *   }
 *   local-failover {
 *     provider = "jlama"
 *     model-name = "tjake/Llama-3.2-1B-Instruct-JQ4"
 *   }
 * }
 * }</pre>
 *
 * <p>To track when these failovers occur, attach a listener during module installation:
 *
 * <pre>{@code
 * install(new LangChain4jModule()
 *   .failoverListener((modelName, error) -> {
 *     log.warn("Model {} failed, switching to fallback. Reason: {}", modelName, error.getMessage());
 *    })
 * );
 * }</pre>
 *
 * <h3>Custom Providers</h3>
 *
 * <p>The extension includes built-in support for popular providers like OpenAI, Anthropic, Ollama,
 * and Jlama. To add support for an unlisted provider (e.g., Google Vertex AI), register a custom
 * {@link io.jooby.langchain4j.ChatModelFactory}:
 *
 * <pre>{@code
 * install(new LangChain4jModule()
 *   .register("vertex", new ChatModelFactory() {
 *     @Override
 *     public ChatModel createChatModel(Config config) {
 *       return VertexAiGeminiChatModel.builder()
 *          .project(config.getString("project"))
 *          .location(config.getString("location"))
 *          .build();
 *     }
 *   }));
 * }</pre>
 *
 * <h3>Dependency Management</h3>
 *
 * <p>To keep your application lightweight, the heavy provider SDKs (like {@code
 * langchain4j-open-ai} or {@code langchain4j-jlama}) are marked as <strong>optional</strong>. You
 * must explicitly add the dependencies for the specific providers you intend to use to your
 * project's {@code pom.xml} or {@code build.gradle}.
 *
 * @author edgar
 * @since 4.1.0
 */
public class LangChain4jModule implements Extension {
  private FailoverListener failoverListener;
  private final Map<String, ChatModelFactory> factories = new HashMap<>();

  /**
   * Registers a custom provider factory.
   *
   * @param provider The provider name (e.g., "vertex").
   * @param factory The factory implementation.
   * @return This module.
   */
  public LangChain4jModule register(String provider, ChatModelFactory factory) {
    factories.put(provider.toLowerCase(), factory);
    return this;
  }

  /**
   * Listener for failover events in a model chain. Called when a primary model fails and the system
   * switches to a fallback.
   *
   * @param failoverListener Failover listener.
   * @return This module.
   */
  public LangChain4jModule failoverListener(FailoverListener failoverListener) {
    this.failoverListener = failoverListener;
    return this;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void install(Jooby app) {
    var config = app.getConfig();

    if (!config.hasPath("langchain4j.models")) {
      throw new IllegalStateException(
          "LangChain4j module installed, but no models found in configuration. Please define at"
              + " least one model under the 'langchain4j.models' key in your application.conf");
    }

    var modelsConfig = config.getConfig("langchain4j.models");
    Map<String, ChatModel> blockingMap = new HashMap<>();
    Map<String, StreamingChatModel> streamingMap = new HashMap<>();

    if (failoverListener == null) {
      failoverListener =
          (name, error) -> {
            LoggerFactory.getLogger(getClass())
                .error("execution of '{}' resulted in exception", name, error);
          };
    }

    // 1. Creation
    for (var name : modelsConfig.root().keySet()) {
      var mConf = modelsConfig.getConfig(name);
      var provider = mConf.getString("provider").toLowerCase();
      var f =
          factories.containsKey(provider)
              ? factories.get(provider)
              : BuiltInModel.resolve(provider);

      blockingMap.put(name, f.createChatModel(mConf));
      streamingMap.put(name, f.createStreamingModel(mConf));
    }

    var services = app.getServices();
    // 2. Chaining & Registration
    for (var name : modelsConfig.root().keySet()) {
      var mConf = modelsConfig.getConfig(name);
      var b = blockingMap.get(name);
      var s = streamingMap.get(name);

      if (mConf.hasPath("fallback")) {
        for (var fbName : mConf.getStringList("fallback")) {
          b = new FallbackChatModel(name, b, blockingMap.get(fbName), failoverListener);
          if (s != null)
            s = new FallbackStreamingChatModel(name, s, streamingMap.get(fbName), failoverListener);
        }
      }

      services.put(ServiceKey.key(ChatModel.class, name), b);
      if (s != null) services.put(ServiceKey.key(StreamingChatModel.class, name), s);

      // Set defaults
      services.putIfAbsent(ChatModel.class, b);
      services.putIfAbsent((Class) b.getClass(), b);
      if (s != null) {
        services.putIfAbsent(StreamingChatModel.class, s);
        services.putIfAbsent((Class) s.getClass(), s);
      }
    }
  }
}
