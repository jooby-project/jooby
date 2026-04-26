/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.output.Output;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

@ExtendWith(MockitoExtension.class)
public class DefaultContextCoverageTest {

  @Mock private Router router;

  @Mock private ValueFactory valueFactory;

  private DefaultContext ctx;
  private Map<String, Object> attributes;
  private Map<String, Object> routerAttributes;

  @BeforeEach
  void setUp() {
    ctx = mock(DefaultContext.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
    attributes = new HashMap<>();
    routerAttributes = new HashMap<>();

    // Lenient stubbing for standard Context dependencies
    lenient().doReturn(router).when(ctx).getRouter();
    lenient().doReturn(attributes).when(ctx).getAttributes();
    lenient().doReturn(valueFactory).when(ctx).getValueFactory();
    lenient().when(router.getAttributes()).thenReturn(routerAttributes);
    lenient().when(router.getRouterOptions()).thenReturn(new RouterOptions());
  }

  @Test
  void requireMethods() {
    ServiceKey<String> key = ServiceKey.key(String.class);
    Reified<String> reified = Reified.get(String.class);

    when(router.require(String.class)).thenReturn("val1");
    when(router.require(String.class, "name")).thenReturn("val2");
    when(router.require(reified)).thenReturn("val3");
    when(router.require(reified, "name")).thenReturn("val4");
    when(router.require(key)).thenReturn("val5");

    assertEquals("val1", ctx.require(String.class));
    assertEquals("val2", ctx.require(String.class, "name"));
    assertEquals("val3", ctx.require(reified));
    assertEquals("val4", ctx.require(reified, "name"));
    assertEquals("val5", ctx.require(key));
  }

  @Test
  void userAttributes() {
    ctx.setUser("johndoe");
    assertEquals("johndoe", ctx.getUser());
    assertEquals("johndoe", attributes.get("user"));
  }

  @Test
  void getAttributeWithFallback() {
    ctx.setAttribute("localKey", "localVal");
    routerAttributes.put("globalKey", "globalVal");

    assertEquals("localVal", ctx.getAttribute("localKey"));
    assertEquals("globalVal", ctx.getAttribute("globalKey"));
    assertNull(ctx.getAttribute("missingKey"));
  }

  @Test
  void matches() {
    doReturn("/path").when(ctx).getRequestPath();
    when(router.match("/pattern", "/path")).thenReturn(true);
    assertTrue(ctx.matches("/pattern"));
  }

  @Test
  void flash() {
    Cookie flashCookie = new Cookie("flash");
    when(router.getFlashCookie()).thenReturn(flashCookie);

    FlashMap flash = ctx.flash();
    assertNotNull(flash);
    assertSame(flash, attributes.get(FlashMap.NAME));

    Value missingVal = mockMissingValue();
    doReturn(missingVal).when(ctx).cookie("flash");
    assertNull(ctx.flashOrNull());

    Value existingVal = mock(Value.class);
    when(existingVal.isMissing()).thenReturn(false);
    doReturn(existingVal).when(ctx).cookie("flash");
    assertNotNull(ctx.flashOrNull());
  }

  @Test
  void session() {
    SessionStore store = mock(SessionStore.class);
    Session sessionMock = mock(Session.class);
    when(router.getSessionStore()).thenReturn(store);

    when(store.findSession(ctx)).thenReturn(null);
    when(store.newSession(ctx)).thenReturn(sessionMock);

    Session session = ctx.session();
    assertNotNull(session);
    assertSame(session, attributes.get(Session.NAME));
  }

  @Test
  void forward() throws Exception {
    Router.Match match = mock(Router.Match.class);
    Route route = mock(Route.class);
    Route.Handler handler = mock(Route.Handler.class);

    when(router.match(ctx)).thenReturn(match);
    when(match.route()).thenReturn(route);
    when(route.getHandler()).thenReturn(handler);
    when(match.execute(ctx, handler)).thenReturn("Result");

    doReturn(ctx).when(ctx).setRequestPath("/forwarded");

    assertEquals("Result", ctx.forward("/forwarded"));
    verify(ctx).setRequestPath("/forwarded");
  }

  @Test
  void lookupSources() {
    Value queryVal = mockMissingValue();
    Value pathVal = mock(Value.class);
    when(pathVal.isMissing()).thenReturn(false);

    doReturn(queryVal).when(ctx).query("id");
    doReturn(pathVal).when(ctx).path("id");

    Value result = ctx.lookup("id", ParamSource.QUERY, ParamSource.PATH);
    assertSame(pathVal, result);

    assertSame(pathVal, ctx.lookup("id", ParamSource.PATH));
    assertTrue(ctx.lookup("id", ParamSource.QUERY).isMissing());
  }

  @Test
  void acceptMatching() {
    Value acceptHeader = mock(Value.class);
    when(acceptHeader.isMissing()).thenReturn(false);
    when(acceptHeader.toList()).thenReturn(Arrays.asList("application/json"));
    doReturn(acceptHeader).when(ctx).header("Accept");

    assertTrue(ctx.accept(MediaType.json));
    assertFalse(ctx.accept(MediaType.html));
  }

  @Test
  void requestURLGeneration() {
    doReturn("https").when(ctx).getScheme();
    doReturn("example.com").when(ctx).getHost();
    doReturn(8080).when(ctx).getPort();
    doReturn("/ctx").when(ctx).getContextPath();
    doReturn("/ctx/api").when(ctx).getRequestPath();
    doReturn("?q=1").when(ctx).queryString();

    assertEquals("https://example.com:8080/ctx/api", ctx.getRequestURL("/ctx/api"));
    assertEquals("https://example.com:8080/ctx/api?q=1", ctx.getRequestURL());
  }

  @Test
  void hostAndPortLogic() {
    doReturn(new ServerOptions().setPort(9090).setHost("0.0.0.0"))
        .when(ctx)
        .require(ServerOptions.class);
    doReturn(false).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn(null);
    doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals("localhost", ctx.getServerHost());
    assertEquals(9090, ctx.getServerPort());
    assertEquals(9090, ctx.getPort());
    assertEquals("localhost", ctx.getHost());
    assertEquals("localhost:9090", ctx.getHostAndPort());
  }

  @Test
  void decodeData() throws Exception {
    Body bodyVal = mock(Body.class);
    doReturn(bodyVal).when(ctx).body();
    when(valueFactory.convert(String.class, bodyVal)).thenReturn("converted");

    assertEquals("converted", ctx.decode(String.class, MediaType.text));

    MessageDecoder decoder = mock(MessageDecoder.class);
    doReturn(decoder).when(ctx).decoder(MediaType.json);
    when(decoder.decode(ctx, Map.class)).thenReturn(Collections.emptyMap());

    assertEquals(Collections.emptyMap(), ctx.decode(Map.class, MediaType.json));
  }

  @Test
  void sendFileDownload() throws Exception {
    FileDownload fd = mock(FileDownload.class);
    when(fd.getContentDisposition()).thenReturn("attachment; filename=test.txt");
    when(fd.getFileSize()).thenReturn(100L);
    when(fd.getContentType()).thenReturn(MediaType.text);

    InputStream stream = new ByteArrayInputStream(new byte[0]);
    when(fd.stream()).thenReturn(stream);

    doReturn(ctx).when(ctx).send(any(InputStream.class));
    doReturn(ctx).when(ctx).setResponseLength(100L);
    doReturn(ctx).when(ctx).setDefaultResponseType(MediaType.text);

    ctx.send(fd);

    verify(ctx).setResponseHeader("Content-Disposition", "attachment; filename=test.txt");
    verify(ctx).send(stream);
  }

  @Test
  void sendPath() throws Exception {
    Path tempPath = Files.createTempFile("jooby-test", ".txt");
    tempPath.toFile().deleteOnExit();

    doReturn(ctx).when(ctx).setDefaultResponseType(MediaType.text);
    doReturn(ctx).when(ctx).send(any(FileChannel.class));

    ctx.send(tempPath);

    verify(ctx).setDefaultResponseType(MediaType.text);
    verify(ctx).send(any(FileChannel.class));
  }

  @Test
  void sendErrorWhenResponseNotStarted() {
    doReturn(false).when(ctx).isResponseStarted();
    doReturn(true).when(ctx).getResetHeadersOnError();
    when(router.errorCode(any())).thenReturn(StatusCode.SERVER_ERROR);

    ErrorHandler errorHandler = mock(ErrorHandler.class);
    when(router.getErrorHandler()).thenReturn(errorHandler);
    when(router.getLog()).thenReturn(mock(Logger.class));

    ctx.sendError(new IllegalArgumentException("Test Error"));

    verify(ctx).removeResponseHeaders();
    verify(ctx).setResponseCode(StatusCode.SERVER_ERROR);
    verify(errorHandler)
        .apply(eq(ctx), any(IllegalArgumentException.class), eq(StatusCode.SERVER_ERROR));
  }

  @Test
  void renderData() throws Exception {
    Route route = mock(Route.class);
    MessageEncoder encoder = mock(MessageEncoder.class);

    var output = mock(Output.class);
    doReturn(route).when(ctx).getRoute();
    when(route.getEncoder()).thenReturn(encoder);
    when(encoder.encode(ctx, "data")).thenReturn(output);

    doReturn(ctx).when(ctx).send(output);

    ctx.render("data");

    verify(ctx).send(output);
  }

  private Value mockMissingValue() {
    Value val = mock(Value.class);
    lenient().when(val.isMissing()).thenReturn(true);
    return val;
  }
}
