/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.langchain4j;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.jooby.langchain4j.FailoverListener;

/**
 * Decorator for {@link StreamingChatModel} handling asynchronous failover. Fallback triggers only
 * if the error occurs before the first token is received.
 */
public class FallbackStreamingChatModel implements StreamingChatModel {
  private final String name;
  private final StreamingChatModel primary;
  private final StreamingChatModel fallback;
  private final FailoverListener fallbackListener;

  public FallbackStreamingChatModel(
      String name,
      StreamingChatModel primary,
      StreamingChatModel fallback,
      FailoverListener fallbackListener) {
    this.name = name;
    this.primary = primary;
    this.fallback = fallback;
    this.fallbackListener = fallbackListener;
  }

  @Override
  public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
    primary.chat(
        request,
        new StreamingChatResponseHandler() {
          private boolean started = false;

          @Override
          public void onPartialResponse(String token) {
            started = true;
            handler.onPartialResponse(token);
          }

          @Override
          public void onCompleteResponse(
              dev.langchain4j.model.chat.response.ChatResponse response) {
            handler.onCompleteResponse(response);
          }

          @Override
          public void onError(Throwable error) {
            if (!started) {
              fallbackListener.onFailover(name, error);
              fallback.chat(request, handler);
            } else {
              handler.onError(error);
            }
          }
        });
  }
}
