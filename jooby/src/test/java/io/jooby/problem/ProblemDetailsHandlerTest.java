/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import com.typesafe.config.Config;
import io.jooby.*;
import io.jooby.exception.NotAcceptableException;

class ProblemDetailsHandlerTest {

  private ProblemDetailsHandler handler;
  private Context ctx;
  private Router router;
  private Logger log;

  @BeforeEach
  void setUp() {
    handler = new ProblemDetailsHandler();
    ctx = mock(Context.class);
    router = mock(Router.class);
    log = mock(Logger.class);

    when(ctx.getRouter()).thenReturn(router);
    when(router.getLog()).thenReturn(log);

    // Mock these to prevent DefaultErrorHandler from crashing
    when(ctx.getMethod()).thenReturn("GET");
    when(ctx.getRequestPath()).thenReturn("/test");
    when(ctx.getRequestType(any())).thenReturn(MediaType.json);

    // Default accept behavior
    when(ctx.accept(anyList())).thenReturn(MediaType.html);
    when(ctx.setResponseType(any(MediaType.class))).thenReturn(ctx);
    when(ctx.setResponseCode(anyInt())).thenReturn(ctx);
    when(ctx.setResponseCode(any(StatusCode.class))).thenReturn(ctx);
  }

  @Test
  @DisplayName("Verify static from(Config) parses full configuration")
  void testFromConfig() {
    Config conf = mock(Config.class);
    Config problemConfig = mock(Config.class);

    when(conf.hasPath(ProblemDetailsHandler.ROOT_CONFIG_PATH)).thenReturn(true);
    when(conf.getConfig(ProblemDetailsHandler.ROOT_CONFIG_PATH)).thenReturn(problemConfig);

    when(problemConfig.hasPath("log4xxErrors")).thenReturn(true);
    when(problemConfig.getBoolean("log4xxErrors")).thenReturn(true);

    when(problemConfig.hasPath("muteCodes")).thenReturn(true);
    when(problemConfig.getIntList("muteCodes")).thenReturn(List.of(404));

    when(problemConfig.hasPath("muteTypes")).thenReturn(true);
    when(problemConfig.getStringList("muteTypes"))
        .thenReturn(List.of("java.lang.IllegalArgumentException"));

    ProblemDetailsHandler result = ProblemDetailsHandler.from(conf);
    assertNotNull(result);
  }

  @Test
  @DisplayName("Verify NotAcceptableException triggers immediate HTML response")
  void testApplyNotAcceptable() {
    NotAcceptableException ex = new NotAcceptableException("No match");
    handler.apply(ctx, ex, StatusCode.NOT_ACCEPTABLE);

    verify(ctx).setResponseType(MediaType.html);
    verify(ctx).send(contains("<h1>Not Acceptable</h1>"));
  }

  @Test
  @DisplayName("Verify JSON content negotiation and problem response mapping")
  @SuppressWarnings("unchecked")
  void testApplyJsonProblem() {
    when(ctx.accept(anyList())).thenReturn(MediaType.json);
    IllegalArgumentException ex = new IllegalArgumentException("Invalid ID");

    handler.apply(ctx, ex, StatusCode.BAD_REQUEST);

    verify(ctx).setResponseType(MediaType.PROBLEM_JSON);
    ArgumentCaptor<Object> resultCaptor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).render(resultCaptor.capture());

    Map<String, Object> map = (Map<String, Object>) resultCaptor.getValue();
    assertEquals("Bad Request", map.get("title"));
    assertEquals("Invalid ID", map.get("detail"));
  }

  @Test
  @DisplayName("Verify XML content negotiation")
  void testApplyXmlProblem() {
    when(ctx.accept(anyList())).thenReturn(MediaType.xml);
    handler.apply(ctx, new Exception("XML Error"), StatusCode.BAD_REQUEST);

    verify(ctx).setResponseType(MediaType.PROBLEM_XML);
    verify(ctx).render(any());
  }

  @Test
  @DisplayName("Verify Plain Text content negotiation")
  void testApplyTextProblem() {
    when(ctx.accept(anyList())).thenReturn(MediaType.text);
    handler.apply(ctx, new Exception("Text Error"), StatusCode.BAD_REQUEST);

    verify(ctx).setResponseType(MediaType.text);
    verify(ctx).send(contains("title='Bad Request'"));
    verify(ctx).send(contains("Text Error"));
  }

  @Test
  @DisplayName("Verify internal error fallback when render fails")
  void testApplyRenderFailureFallback() {
    when(ctx.accept(anyList())).thenReturn(MediaType.json);
    doThrow(new NotAcceptableException("foo")).when(ctx).render(any());

    handler.apply(ctx, new Exception("fail"), StatusCode.BAD_REQUEST);

    verify(ctx, atLeastOnce()).setResponseType(MediaType.html);
  }

  @Test
  @DisplayName("Evaluate problem: HttpProblem instance")
  void testEvaluateHttpProblem() {
    HttpProblem problem = HttpProblem.valueOf(StatusCode.FORBIDDEN, "Forbidden", "Access Denied");
    handler.apply(ctx, problem, StatusCode.FORBIDDEN);

    // apply() calls setResponseCode, and fallback sendHtml() also calls setResponseCode
    verify(ctx, atLeastOnce()).setResponseCode(403);
    verify(ctx).send(contains("Access Denied"));
  }

  @Test
  @DisplayName("Evaluate problem: 500 error mapping")
  void testEvaluateInternalError() {
    handler.apply(ctx, new RuntimeException("boom"), StatusCode.SERVER_ERROR);
    verify(ctx).send(contains("Server Error"));
  }

  @Test
  @DisplayName("Evaluate problem: message multi-line splitting")
  void testEvaluateMessageSplitting() {
    handler.apply(ctx, new Exception("First Line\nSecond Line"), StatusCode.BAD_REQUEST);
    verify(ctx).send(contains("First Line"));
  }

  @Test
  @DisplayName("Logging: Server Error (Error level)")
  void testLogServerError() {
    handler.apply(ctx, new Exception("critical"), StatusCode.SERVER_ERROR);
    verify(log, atLeastOnce()).error(anyString(), any(Throwable.class));
  }

  @Test
  @DisplayName("Logging: 4xx Errors (Info/Debug level)")
  void testLog4xxError() {
    handler.log4xxErrors();
    when(log.isDebugEnabled()).thenReturn(false);

    handler.apply(ctx, new Exception("client error"), StatusCode.BAD_REQUEST);
    verify(log).info(anyString());

    reset(log);
    when(log.isDebugEnabled()).thenReturn(true);
    handler.apply(ctx, new Exception("client error debug"), StatusCode.BAD_REQUEST);
    verify(log).debug(anyString(), any(Throwable.class));
  }

  // Dummy exception class that correctly extends Throwable to satisfy the type constraints
  private static class MappableException extends RuntimeException implements HttpProblemMappable {
    private final HttpProblem problem;

    public MappableException(HttpProblem problem) {
      this.problem = problem;
    }

    @Override
    public HttpProblem toHttpProblem() {
      return problem;
    }
  }

  @Test
  @DisplayName("HTML Generation: verify extra details (instance, params, errors)")
  void testHtmlExtraDetails() {
    HttpProblem problem =
        HttpProblem.builder()
            .status(StatusCode.BAD_REQUEST)
            .title("Bad Request")
            .instance(URI.create("/path"))
            .detail("Some details")
            .param("p1", "v1")
            .errors(List.of(new HttpProblem.Error("err1", "err1 detail")))
            .build();

    // Use our custom dummy exception to pass the Type constraints
    MappableException mappableException = new MappableException(problem);

    handler.apply(ctx, mappableException, StatusCode.BAD_REQUEST);

    verify(ctx)
        .send(
            argThat(
                (String s) ->
                    s.contains("instance: /path")
                        && s.contains("detail: Some details")
                        && s.contains("parameters: {p1=v1}")
                        // Match the class name since toString() prints the object reference
                        && s.contains("errors: [io.jooby.problem.HttpProblem$Error@")));
  }

  @Test
  @DisplayName("Verify apply handles unexpected internal exceptions silently")
  void testApplyUnexpectedException() {
    when(ctx.accept(anyList())).thenThrow(new RuntimeException("Negotiation Crash"));

    assertDoesNotThrow(() -> handler.apply(ctx, new Exception("original"), StatusCode.BAD_REQUEST));
    verify(log).error(contains("Unexpected error"), any(Throwable.class));
  }
}
