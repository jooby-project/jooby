/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.jooby.langchain4j.FailoverListener;

/**
 * Decorator for {@link ChatModel} that provides failover logic. Catching exceptions from the
 * primary model and routing to a fallback instance.
 */
public class FallbackChatModel implements ChatModel {
  private final String name;
  private final ChatModel primary;
  private final ChatModel fallback;
  private final FailoverListener listener;

  public FallbackChatModel(
      String name, ChatModel primary, ChatModel fallback, FailoverListener listener) {
    this.name = name;
    this.primary = primary;
    this.fallback = fallback;
    this.listener = listener;
  }

  @Override
  public ChatResponse chat(ChatRequest request) {
    try {
      return primary.chat(request);
    } catch (Exception e) {
      listener.onFailover(name, e);
      return fallback.chat(request);
    }
  }
}
