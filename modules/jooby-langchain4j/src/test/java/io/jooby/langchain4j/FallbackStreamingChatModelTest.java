/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.langchain4j;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

class FallbackStreamingChatModelTest {

  private ChatRequest createValidRequest() {
    return ChatRequest.builder().messages(UserMessage.from("Hello")).build();
  }

  @Test
  @DisplayName("Should trigger fallback if primary fails before first token")
  void failsBeforeTokens() {
    StreamingChatModel primary = mock(StreamingChatModel.class);
    StreamingChatModel fallback = mock(StreamingChatModel.class);
    FailoverListener listener = mock(FailoverListener.class);
    StreamingChatResponseHandler originalHandler = mock(StreamingChatResponseHandler.class);
    ChatRequest request = createValidRequest();

    doAnswer(
            invocation -> {
              StreamingChatResponseHandler internalHandler = invocation.getArgument(1);
              internalHandler.onError(new RuntimeException("Connection Timeout"));
              return null;
            })
        .when(primary)
        .chat(eq(request), any());

    FallbackStreamingChatModel model =
        new FallbackStreamingChatModel("stream-ai", primary, fallback, listener);
    model.chat(request, originalHandler);

    verify(listener).onFailover(eq("stream-ai"), any(RuntimeException.class));
    verify(fallback).chat(eq(request), eq(originalHandler));
    verify(originalHandler, never()).onError(any());
  }

  @Test
  @DisplayName("Should NOT trigger fallback if primary fails mid-stream")
  void failsMidStream() {
    StreamingChatModel primary = mock(StreamingChatModel.class);
    StreamingChatModel fallback = mock(StreamingChatModel.class);
    FailoverListener listener = mock(FailoverListener.class);
    StreamingChatResponseHandler originalHandler = mock(StreamingChatResponseHandler.class);
    ChatRequest request = createValidRequest();

    doAnswer(
            invocation -> {
              StreamingChatResponseHandler internalHandler = invocation.getArgument(1);
              internalHandler.onPartialResponse("Hello");
              internalHandler.onError(new RuntimeException("Connection Dropped"));
              return null;
            })
        .when(primary)
        .chat(eq(request), any());

    FallbackStreamingChatModel model =
        new FallbackStreamingChatModel("stream-ai", primary, fallback, listener);
    model.chat(request, originalHandler);

    verify(originalHandler).onPartialResponse("Hello");
    verifyNoInteractions(listener);
    verifyNoInteractions(fallback);
    verify(originalHandler).onError(any(RuntimeException.class));
  }

  @Test
  @DisplayName("Should pass successful completion down to original handler")
  void successfulCompletion() {
    StreamingChatModel primary = mock(StreamingChatModel.class);
    StreamingChatModel fallback = mock(StreamingChatModel.class);
    FailoverListener listener = mock(FailoverListener.class);
    StreamingChatResponseHandler originalHandler = mock(StreamingChatResponseHandler.class);
    ChatRequest request = createValidRequest();

    // Mock the response to bypass builder validation
    ChatResponse completeResponse = mock(ChatResponse.class);

    doAnswer(
            invocation -> {
              StreamingChatResponseHandler internalHandler = invocation.getArgument(1);
              internalHandler.onPartialResponse("Done");
              internalHandler.onCompleteResponse(completeResponse);
              return null;
            })
        .when(primary)
        .chat(eq(request), any());

    FallbackStreamingChatModel model =
        new FallbackStreamingChatModel("stream-ai", primary, fallback, listener);
    model.chat(request, originalHandler);

    verify(originalHandler).onPartialResponse("Done");
    verify(originalHandler).onCompleteResponse(completeResponse);
    verifyNoInteractions(fallback);
  }
}
