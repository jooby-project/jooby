/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.langchain4j;

import java.nio.file.Paths;
import java.time.Duration;

import com.typesafe.config.Config;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.jlama.JlamaChatModel;
import dev.langchain4j.model.jlama.JlamaStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.langchain4j.ChatModelFactory;

/**
 * Enumeration of built-in LangChain4j model providers supported by the Jooby extension. Each
 * constant implements {@link ChatModelFactory} to provide provider-specific instantiation logic.
 */
public enum BuiltInModel implements ChatModelFactory {
  OPENAI {
    @Override
    public ChatModel createChatModel(@NonNull Config config) {
      check("dev.langchain4j.model.openai.OpenAiChatModel", "langchain4j-open-ai");
      return OpenAiChatModel.builder()
          .apiKey(config.getString("api-key"))
          .modelName(config.hasPath("model-name") ? config.getString("model-name") : "gpt-4o-mini")
          .timeout(getTimeout(config, Duration.ofSeconds(60)))
          .temperature(getTemp(config))
          .build();
    }

    @Override
    public StreamingChatModel createStreamingModel(@NonNull Config config) {
      return OpenAiStreamingChatModel.builder()
          .apiKey(config.getString("api-key"))
          .modelName(config.hasPath("model-name") ? config.getString("model-name") : "gpt-4o-mini")
          .timeout(getStreamTimeout(config))
          .temperature(getTemp(config))
          .build();
    }
  },

  ANTHROPIC {
    @Override
    public ChatModel createChatModel(@NonNull Config config) {
      check("dev.langchain4j.model.anthropic.AnthropicChatModel", "langchain4j-anthropic");
      return AnthropicChatModel.builder()
          .apiKey(config.getString("api-key"))
          .modelName(
              config.hasPath("model-name")
                  ? config.getString("model-name")
                  : "claude-3-5-sonnet-latest")
          .timeout(getTimeout(config, Duration.ofSeconds(60)))
          .temperature(getTemp(config))
          .build();
    }

    @Override
    public StreamingChatModel createStreamingModel(@NonNull Config config) {
      return AnthropicStreamingChatModel.builder()
          .apiKey(config.getString("api-key"))
          .modelName(
              config.hasPath("model-name")
                  ? config.getString("model-name")
                  : "claude-3-5-sonnet-latest")
          .timeout(getStreamTimeout(config))
          .temperature(getTemp(config))
          .build();
    }
  },

  OLLAMA {
    @Override
    public ChatModel createChatModel(@NonNull Config config) {
      check("dev.langchain4j.model.ollama.OllamaChatModel", "langchain4j-ollama");
      return OllamaChatModel.builder()
          .baseUrl(config.getString("base-url"))
          .modelName(config.getString("model-name"))
          .timeout(getTimeout(config, Duration.ofSeconds(60)))
          .build();
    }

    @Override
    public StreamingChatModel createStreamingModel(@NonNull Config config) {
      return OllamaStreamingChatModel.builder()
          .baseUrl(config.getString("base-url"))
          .modelName(config.getString("model-name"))
          .timeout(getStreamTimeout(config))
          .build();
    }
  },

  JLAMA {
    @Override
    public ChatModel createChatModel(@NonNull Config config) {
      check("dev.langchain4j.model.jlama.JlamaChatModel", "langchain4j-jlama");
      return JlamaChatModel.builder()
          .modelName(config.getString("model-name"))
          .workingDirectory(
              config.hasPath("working-dir")
                  ? Paths.get(config.getString("working-dir"))
                  : Paths.get(System.getProperty("user.dir"), "./models"))
          .build();
    }

    @Override
    public StreamingChatModel createStreamingModel(@NonNull Config config) {
      return JlamaStreamingChatModel.builder()
          .modelName(config.getString("model-name"))
          .workingDirectory(
              config.hasPath("working-dir")
                  ? Paths.get(config.getString("working-dir"))
                  : Paths.get(System.getProperty("user.dir"), "./models"))
          .build();
    }
  };

  /**
   * Resolves a built-in provider by name.
   *
   * @param name The provider name (e.g. "openai").
   * @return The corresponding enum constant.
   * @throws IllegalArgumentException if provider is unknown.
   */
  public static BuiltInModel resolve(String name) {
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unsupported LangChain4j provider: " + name);
    }
  }

  // --- Helper Methods for Enum Implementation ---

  protected void check(String className, String artifact) {
    try {
      Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(
          "Provider dependency missing. Add 'dev.langchain4j:" + artifact + "' to your project.");
    }
  }

  protected Duration getTimeout(Config config, Duration defaultValue) {
    return config.hasPath("timeout") ? config.getDuration("timeout") : defaultValue;
  }

  protected Duration getStreamTimeout(Config config) {
    return config.hasPath("streaming-timeout")
        ? config.getDuration("streaming-timeout")
        : Duration.ofSeconds(10);
  }

  protected double getTemp(Config config) {
    return config.hasPath("temperature") ? config.getDouble("temperature") : 0.7;
  }
}
