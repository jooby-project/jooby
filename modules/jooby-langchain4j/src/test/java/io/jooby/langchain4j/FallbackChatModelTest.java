/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.langchain4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

class FallbackChatModelTest {

  private ChatRequest createValidRequest() {
    return ChatRequest.builder().messages(UserMessage.from("Hello")).build();
  }

  @Test
  @DisplayName("Should return primary response when primary succeeds")
  void primarySucceeds() {
    ChatModel primary = mock(ChatModel.class);
    ChatModel fallback = mock(ChatModel.class);
    FailoverListener listener = mock(FailoverListener.class);
    ChatRequest request = createValidRequest();

    // Mock the response to bypass builder validation
    ChatResponse response = mock(ChatResponse.class);

    when(primary.chat(request)).thenReturn(response);

    FallbackChatModel model = new FallbackChatModel("test-model", primary, fallback, listener);
    ChatResponse result = model.chat(request);

    assertEquals(response, result);
    verify(primary).chat(request);
    verifyNoInteractions(fallback);
    verifyNoInteractions(listener);
  }

  @Test
  @DisplayName("Should trigger listener and use fallback when primary fails")
  void primaryFailsFallbackSucceeds() {
    ChatModel primary = mock(ChatModel.class);
    ChatModel fallback = mock(ChatModel.class);
    FailoverListener listener = mock(FailoverListener.class);
    ChatRequest request = createValidRequest();

    // Mock the response to bypass builder validation
    ChatResponse fallbackResponse = mock(ChatResponse.class);
    RuntimeException apiError = new RuntimeException("API Rate Limit");

    when(primary.chat(request)).thenThrow(apiError);
    when(fallback.chat(request)).thenReturn(fallbackResponse);

    FallbackChatModel model = new FallbackChatModel("test-model", primary, fallback, listener);
    ChatResponse result = model.chat(request);

    assertEquals(fallbackResponse, result);
    verify(listener).onFailover("test-model", apiError);
    verify(fallback).chat(request);
  }

  @Test
  @DisplayName("Should throw exception if both primary and fallback fail")
  void bothFail() {
    ChatModel primary = mock(ChatModel.class);
    ChatModel fallback = mock(ChatModel.class);
    FailoverListener listener = mock(FailoverListener.class);
    ChatRequest request = createValidRequest();

    when(primary.chat(request)).thenThrow(new RuntimeException("Primary Down"));
    when(fallback.chat(request)).thenThrow(new RuntimeException("Fallback Down"));

    FallbackChatModel model = new FallbackChatModel("test-model", primary, fallback, listener);

    assertThrows(RuntimeException.class, () -> model.chat(request));
    verify(listener).onFailover(eq("test-model"), any(RuntimeException.class));
  }
}
