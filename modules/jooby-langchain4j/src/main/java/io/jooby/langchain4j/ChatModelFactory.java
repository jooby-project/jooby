/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.langchain4j;

import com.typesafe.config.Config;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Factory contract for creating LangChain4j chat models from Jooby configuration. Implementations
 * map {@link Config} keys to specific model builder methods.
 *
 * @author edgar
 * @since 1.0.0
 */
public interface ChatModelFactory {

  /**
   * Creates a blocking chat model.
   *
   * @param config The configuration block for this model.
   * @return A non-null instance of a {@link ChatModel}.
   */
  ChatModel createChatModel(@NonNull Config config);

  /**
   * Creates a streaming chat model. Returns {@code null} if the provider does not support
   * streaming.
   *
   * @param config The configuration block for this model.
   * @return A {@link StreamingChatModel} or {@code null}.
   */
  @Nullable default StreamingChatModel createStreamingModel(@NonNull Config config) {
    return null;
  }
}
