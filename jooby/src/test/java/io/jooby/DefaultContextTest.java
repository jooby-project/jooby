/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static io.jooby.Context.RFC1123;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

@ExtendWith(MockitoExtension.class)
public class DefaultContextTest {

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

    lenient().doReturn(router).when(ctx).getRouter();
    lenient().doReturn(attributes).when(ctx).getAttributes();
    lenient().doReturn(valueFactory).when(ctx).getValueFactory();
    lenient().when(router.getAttributes()).thenReturn(routerAttributes);
    lenient().when(router.getRouterOptions()).thenReturn(new RouterOptions());
    lenient().when(router.getValueFactory()).thenReturn(valueFactory);
  }

  // --- Dependency Injection / Require Methods ---

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

  // --- Attributes and User ---

  @Test
  void userAttributes() {
    ctx.setUser("johndoe");
    assertEquals("johndoe", ctx.getUser());
    assertEquals("johndoe", attributes.get("user"));
  }

  @Test
  void attributesLogic() {
    ctx.setAttribute("localKey", "localVal");
    routerAttributes.put("globalKey", "globalVal");

    assertEquals("localVal", ctx.getAttribute("localKey"));
    assertEquals("globalVal", ctx.getAttribute("globalKey"));
    assertNull(ctx.getAttribute("missingKey"));
  }

  // --- Matching and Forwarding ---

  @Test
  void matches() {
    doReturn("/path").when(ctx).getRequestPath();
    when(router.match("/pattern", "/path")).thenReturn(true);
    assertTrue(ctx.matches("/pattern"));
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
  void forwardException() {
    doReturn(ctx).when(ctx).setRequestPath("/forwarded");
    when(router.match(ctx)).thenThrow(new RuntimeException("Forward Failed"));

    assertThrows(RuntimeException.class, () -> ctx.forward("/forwarded"));
  }

  // --- Flash Scope ---

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

    // flash(String) and flash(String, String)
    flash.put("key", "val");
    Value flashVal = ctx.flash("key");
    assertFalse(flashVal.isMissing());
    assertEquals("val", flashVal.value());

    Value missingFlash = ctx.flash("missingKey", "defaultVal");
    assertEquals("defaultVal", missingFlash.value());
    Value presentFlash = ctx.flash("key", "defaultVal");
    assertEquals("val", presentFlash.value());
  }

  // --- Session Scope ---

  @Test
  void session() {
    SessionStore store = mock(SessionStore.class);
    Session sessionMock = mock(Session.class);
    when(router.getSessionStore()).thenReturn(store);

    when(store.findSession(ctx)).thenReturn(null);
    when(store.newSession(ctx)).thenReturn(sessionMock);

    // Initial creation
    Session session = ctx.session();
    assertNotNull(session);
    assertSame(session, attributes.get(Session.NAME));

    // session(String) and session(String, String)
    Value mockVal = mock(Value.class);
    when(sessionMock.get("key")).thenReturn(mockVal);
    assertSame(mockVal, ctx.session("key"));

    // FIX: Extract the missing value creation BEFORE the when() chain
    Value missingVal = mockMissingValue();
    when(sessionMock.get("missingKey")).thenReturn(missingVal);

    Value defaultVal = ctx.session("missingKey", "def");
    assertEquals("def", defaultVal.value());
    Value presentValue = ctx.session("key", "def");
    assertEquals(mockVal, presentValue);
  }

  @Test
  void sessionOrNullExisting() {
    SessionStore store = mock(SessionStore.class);
    Session sessionMock = mock(Session.class);
    when(router.getSessionStore()).thenReturn(store);
    when(store.findSession(ctx)).thenReturn(sessionMock);

    Session result = ctx.sessionOrNull();
    assertSame(sessionMock, result);
    assertSame(sessionMock, attributes.get(Session.NAME));
  }

  @Test
  void sessionMissingValues() {
    // Session is null -> session(String) returns missing, session(String, String) returns default
    when(router.getSessionStore()).thenReturn(mock(SessionStore.class));

    assertTrue(ctx.session("key").isMissing());
    assertEquals("def", ctx.session("key", "def").value());
  }

  // --- Parameter Lookup ---

  @Test
  void lookupAndSources() {
    Value queryVal = mockMissingValue();
    Value pathVal = mock(Value.class);
    when(pathVal.isMissing()).thenReturn(false);

    doReturn(queryVal).when(ctx).query("id");
    doReturn(pathVal).when(ctx).path("id");

    var result = ctx.lookup("id", ParamSource.QUERY, ParamSource.PATH);
    assertSame(pathVal, result);

    assertSame(pathVal, ctx.lookup("id", ParamSource.PATH));
    assertTrue(ctx.lookup("id", ParamSource.QUERY).isMissing());

    assertFalse(ctx.lookup("id").isMissing());

    // ParamLookup interface
    assertNotNull(ctx.lookup());
  }

  // --- Values (Cookies, Path, Query, Header, Form, Body) ---

  @Test
  void cookieMapMethods() {
    Map<String, String> cookies = new HashMap<>();
    cookies.put("foo", "bar");
    doReturn(cookies).when(ctx).cookieMap();

    assertEquals("bar", ctx.cookie("foo").value());
    assertEquals("def", ctx.cookie("missing", "def").value());
    assertEquals("bar", ctx.cookie("foo", "def").value());
  }

  @Test
  void pathMapMethods() {
    Map<String, String> pathMap = new HashMap<>();
    pathMap.put("id", "123");
    doReturn(pathMap).when(ctx).pathMap();

    assertEquals("123", ctx.path("id").value());
    assertTrue(ctx.path("missing").isMissing());

    Value fullPath = ctx.path();
    assertEquals("123", fullPath.get("id").value());
  }

  @Test
  void pathBean() {
    var uuid = UUID.randomUUID();
    var path = mock(Value.class);
    when(path.to(UUID.class)).thenReturn(uuid);
    doReturn(path).when(ctx).path();

    assertEquals(uuid, ctx.path(UUID.class));
  }

  @Test
  void queryMethods() {
    var queryNode = mock(QueryString.class);
    doReturn(queryNode).when(ctx).query();

    Value singleVal = mock(Value.class);
    when(queryNode.get("id")).thenReturn(singleVal);
    when(queryNode.getOrDefault("id", "def")).thenReturn(singleVal);
    when(queryNode.queryString()).thenReturn("?id=1");
    when(queryNode.toEmpty(String.class)).thenReturn("obj");

    Map<String, String> map = new HashMap<>();
    when(queryNode.toMap()).thenReturn(map);

    assertSame(singleVal, ctx.query("id"));
    assertSame(singleVal, ctx.query("id", "def"));
    assertEquals("?id=1", ctx.queryString());
    assertEquals("obj", ctx.query(String.class));
    assertSame(map, ctx.queryMap());
  }

  @Test
  void headerMethods() {
    var headerNode = mock(Value.class);
    doReturn(headerNode).when(ctx).header();

    Value singleVal = mock(Value.class);
    when(headerNode.get("id")).thenReturn(singleVal);
    when(headerNode.getOrDefault("id", "def")).thenReturn(singleVal);
    Map<String, String> map = new HashMap<>();
    when(headerNode.toMap()).thenReturn(map);

    assertSame(singleVal, ctx.header("id"));
    assertSame(singleVal, ctx.header("id", "def"));
    assertSame(map, ctx.headerMap());
  }

  @Test
  void formAndFilesMethods() {
    var formNode = mock(Formdata.class);
    doReturn(formNode).when(ctx).form();

    Value singleVal = mock(Value.class);
    when(formNode.get("id")).thenReturn(singleVal);
    when(formNode.getOrDefault("id", "def")).thenReturn(singleVal);
    when(formNode.to(String.class)).thenReturn("obj");
    Map<String, String> map = new HashMap<>();
    when(formNode.toMap()).thenReturn(map);

    FileUpload file = mock(FileUpload.class);
    List<FileUpload> files = Arrays.asList(file);
    when(formNode.files()).thenReturn(files);
    when(formNode.files("f")).thenReturn(files);
    when(formNode.file("f")).thenReturn(file);

    assertSame(singleVal, ctx.form("id"));
    assertSame(singleVal, ctx.form("id", "def"));
    assertEquals("obj", ctx.form(String.class));
    assertSame(map, ctx.formMap());
    assertSame(files, ctx.files());
    assertSame(files, ctx.files("f"));
    assertSame(file, ctx.file("f"));
  }

  @Test
  void bodyMethods() {
    Body bodyNode = mock(Body.class);
    doReturn(bodyNode).when(ctx).body();
    when(bodyNode.to(String.class)).thenReturn("obj");
    when(bodyNode.to((Type) String.class)).thenReturn("obj");

    assertEquals("obj", ctx.body(String.class));
    assertEquals("obj", ctx.body((Type) String.class));
  }

  // --- Content Negotiation ---

  @Test
  void acceptMatching() {
    Value acceptHeader = mock(Value.class);
    when(acceptHeader.isMissing()).thenReturn(false);
    when(acceptHeader.toList()).thenReturn(Arrays.asList("application/json", "text/html"));
    doReturn(acceptHeader).when(ctx).header("Accept");

    assertTrue(ctx.accept(MediaType.json));
    assertFalse(ctx.accept(MediaType.xml));

    assertEquals(MediaType.json, ctx.accept(Arrays.asList(MediaType.xml, MediaType.json)));
    assertEquals(
        MediaType.json,
        ctx.accept(Arrays.asList(MediaType.html, MediaType.json))); // Order preserves match index
  }

  @Test
  void acceptMissingHeader() {
    doReturn(mockMissingValue()).when(ctx).header("Accept");

    assertEquals(MediaType.json, ctx.accept(singletonList(MediaType.json)));
    assertNull(ctx.accept(Collections.emptyList()));
  }

  // --- URL and Host/Port extraction ---

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
  void requestURLGenerationOnDefaultPort() {
    doReturn("http").when(ctx).getScheme();
    doReturn("example.com").when(ctx).getHost();
    doReturn(80).when(ctx).getPort();
    doReturn("/ctx").when(ctx).getContextPath();
    doReturn("/ctx/api").when(ctx).getRequestPath();
    doReturn("?q=1").when(ctx).queryString();

    assertEquals("http://example.com/ctx/some/api", ctx.getRequestURL("/some/api"));
    assertEquals("http://example.com/ctx/api?q=1", ctx.getRequestURL());
  }

  @Test
  void requestURLGenerationOnDefaultSecuretPort() {
    doReturn("https").when(ctx).getScheme();
    doReturn("example.com").when(ctx).getHost();
    doReturn(443).when(ctx).getPort();
    doReturn("/ctx").when(ctx).getContextPath();
    doReturn("/ctx/api").when(ctx).getRequestPath();
    doReturn("?q=1").when(ctx).queryString();

    assertEquals("https://example.com/ctx/some/api", ctx.getRequestURL("/some/api"));
    assertEquals("https://example.com/ctx/api?q=1", ctx.getRequestURL());
  }

  @Test
  void requestURLVariations() {
    doReturn("https").when(ctx).getScheme();
    doReturn("some-host").when(ctx).getHost();
    doReturn(443).when(ctx).getPort(); // Default HTTPS port, should be omitted
    doReturn("/").when(ctx).getContextPath();
    doReturn("/path").when(ctx).getRequestPath();
    doReturn("?query").when(ctx).queryString();

    assertEquals("https://some-host/path?query", ctx.getRequestURL());
    assertEquals("https://some-host/my-path", ctx.getRequestURL("/my-path"));
  }

  @Test
  void getRequestTypeAndLength() {
    Value typeVal = mock(Value.class);
    when(typeVal.isMissing()).thenReturn(false);
    when(typeVal.value()).thenReturn("application/json");

    Value lengthVal = mock(Value.class);
    when(lengthVal.isMissing()).thenReturn(false);
    when(lengthVal.longValue()).thenReturn(1024L);

    doReturn(typeVal).when(ctx).header("Content-Type");
    doReturn(lengthVal).when(ctx).header("Content-Length");

    assertEquals(MediaType.json, ctx.getRequestType());
    assertEquals(MediaType.json, ctx.getRequestType(MediaType.html));
    assertEquals(1024L, ctx.getRequestLength());
  }

  @Test
  void getRequestTypeAndLengthMissing() {
    doReturn(mockMissingValue()).when(ctx).header("Content-Type");
    doReturn(mockMissingValue()).when(ctx).header("Content-Length");

    assertNull(ctx.getRequestType());
    assertEquals(MediaType.html, ctx.getRequestType(MediaType.html));
    assertEquals(-1L, ctx.getRequestLength());
  }

  @Test
  void hostAndPortLogic() {
    ServerOptions options = new ServerOptions().setPort(9090).setHost("0.0.0.0");
    doReturn(options).when(ctx).require(ServerOptions.class);
    doReturn(false).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn(null);
    doReturn(mockHostHeader).when(ctx).header("Host");

    // No headers, trust proxy false -> fallbacks
    assertEquals("localhost:9090", ctx.getHostAndPort());
    assertEquals("localhost", ctx.getServerHost());
    assertEquals(9090, ctx.getServerPort());
    assertEquals(9090, ctx.getPort());
    assertEquals("localhost", ctx.getHost());
  }

  @Test
  void hostAndPortLogicNoRename() {
    ServerOptions options = new ServerOptions().setPort(9090).setHost("localhost");
    doReturn(options).when(ctx).require(ServerOptions.class);
    doReturn(false).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn(null);
    doReturn(mockHostHeader).when(ctx).header("Host");

    // No headers, trust proxy false -> fallbacks
    assertEquals("localhost:9090", ctx.getHostAndPort());
    assertEquals("localhost", ctx.getServerHost());
    assertEquals(9090, ctx.getServerPort());
    assertEquals(9090, ctx.getPort());
    assertEquals("localhost", ctx.getHost());
  }

  @Test
  void hostAndPortLogicWithBracket() {
    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn("[my.host.com]");
    doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals("my.host.com", ctx.getHostAndPort());
  }

  @Test
  void hostAndPortWithHeaders() {
    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn("custom.com:80");
    doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals("custom.com:80", ctx.getHostAndPort());
    assertEquals("custom.com", ctx.getHost());
    assertEquals(80, ctx.getPort());
  }

  @Test
  void hostAndPortWithProxy() {
    when(router.getRouterOptions()).thenReturn(new RouterOptions().setTrustProxy(true));
    Value mockProxyHeader = mock(Value.class);
    when(mockProxyHeader.toOptional()).thenReturn(Optional.of("proxy.com:443, other.com"));
    doReturn(mockProxyHeader).when(ctx).header("X-Forwarded-Host");

    assertEquals("proxy.com:443", ctx.getHostAndPort()); // trims at comma
  }

  @Test
  void hostAndPortIPv6() {
    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn("[::1]");
    doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals("::1", ctx.getHostAndPort());
  }

  @Test
  void securePortFallback() {
    ServerOptions options = new ServerOptions().setPort(8080); // securePort is null
    doReturn(options).when(ctx).require(ServerOptions.class);
    doReturn(true).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn(null);
    lenient().doReturn(mockHostHeader).when(ctx).header("Host");

    // secure port is null, falls back to plain port
    assertEquals(8080, ctx.getServerPort());
    assertEquals(8080, ctx.getPort()); // extracted from server port since hostAndPort has no :
  }

  @Test
  void defaultSecurePort() {
    doReturn(true).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn("localhost");
    lenient().doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals(443, ctx.getPort());
  }

  @Test
  void defaultNonSecurePort() {
    doReturn(false).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn("localhost");
    lenient().doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals(80, ctx.getPort());
  }

  @Test
  void defaultPort() {
    ServerOptions options = new ServerOptions().setPort(8081);
    doReturn(options).when(ctx).require(ServerOptions.class);
    doReturn(false).when(ctx).isSecure();

    Value mockHostHeader = mock(Value.class);
    when(mockHostHeader.valueOrNull()).thenReturn(null);
    lenient().doReturn(mockHostHeader).when(ctx).header("Host");

    assertEquals(8081, ctx.getPort());
  }

  @Test
  void isSecure() {
    doReturn("https").when(ctx).getScheme();
    assertTrue(ctx.isSecure());
    doReturn("http").when(ctx).getScheme();
    assertFalse(ctx.isSecure());
  }

  // --- Decoding & Responses ---

  @Test
  void decodeData() throws Exception {
    Body bodyVal = mock(Body.class);
    doReturn(bodyVal).when(ctx).body();
    when(valueFactory.convert(String.class, bodyVal)).thenReturn("converted");

    assertEquals("converted", ctx.decode(String.class, MediaType.text));

    MessageDecoder decoder = mock(MessageDecoder.class);
    Route route = mock(Route.class);
    doReturn(route).when(ctx).getRoute();
    when(route.decoder(MediaType.json)).thenReturn(decoder);
    when(decoder.decode(ctx, Map.class)).thenReturn(Collections.emptyMap());

    assertEquals(Collections.emptyMap(), ctx.decode(Map.class, MediaType.json));
    assertEquals(decoder, ctx.decoder(MediaType.json));
  }

  @Test
  void decodeException() {
    doReturn(mock(Body.class)).when(ctx).body();
    when(valueFactory.convert(any(), any())).thenThrow(new RuntimeException("Decode Error"));
    assertThrows(RuntimeException.class, () -> ctx.decode(String.class, MediaType.text));
  }

  @Test
  void setResponseHeadersOverloads() {
    Date date = new Date(10000000000L);
    Instant instant = date.toInstant();

    // Use the actual RFC1123 formatter to guarantee a match regardless of timezone
    String expectedDateString = RFC1123.format(instant);

    // Leniently stub the base String overload.
    // This prevents Strict Stubbing complaints when the intermediate Date/Instant overloads are
    // intercepted.
    lenient().doReturn(ctx).when(ctx).setResponseHeader(anyString(), anyString());

    ctx.setResponseHeader("h1", date);
    verify(ctx).setResponseHeader("h1", expectedDateString);

    ctx.setResponseHeader("h2", instant);
    verify(ctx).setResponseHeader("h2", expectedDateString);

    ctx.setResponseHeader("h3", (Object) date);
    verify(ctx).setResponseHeader("h3", expectedDateString);

    ctx.setResponseHeader("h4", (Object) instant);
    verify(ctx).setResponseHeader("h4", expectedDateString);

    ctx.setResponseHeader("h5", (Object) "stringVal");
    verify(ctx).setResponseHeader("h5", "stringVal");
  }

  @Test
  void setResponseCode() {
    doReturn(ctx).when(ctx).setResponseCode(200);
    ctx.setResponseCode(StatusCode.OK);
    verify(ctx).setResponseCode(200);
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

  @Test
  void renderDataNullNotStarted() throws Exception {
    Route route = mock(Route.class);
    MessageEncoder encoder = mock(MessageEncoder.class);

    doReturn(route).when(ctx).getRoute();
    when(route.getEncoder()).thenReturn(encoder);
    when(encoder.encode(ctx, "data")).thenReturn(null);
    doReturn(false).when(ctx).isResponseStarted();

    assertThrows(IllegalStateException.class, () -> ctx.render("data"));
  }

  @Test
  void renderDataException() throws Exception {
    Route route = mock(Route.class);
    doReturn(route).when(ctx).getRoute();
    when(route.getEncoder()).thenThrow(new RuntimeException("Encode fail"));

    assertThrows(RuntimeException.class, () -> ctx.render("data"));
  }

  @Test
  void responseStreamAndWriter() throws Exception {
    OutputStream out = mock(OutputStream.class);
    PrintWriter writer = mock(PrintWriter.class);

    doReturn(ctx).when(ctx).setResponseType(MediaType.json);
    doReturn(out).when(ctx).responseStream();
    doReturn(writer).when(ctx).responseWriter(MediaType.text);

    assertEquals(out, ctx.responseStream(MediaType.json));

    ctx.responseStream(o -> o.write(1));
    verify(out).write(1);

    ctx.responseStream(MediaType.json, o -> o.write(2));
    verify(out).write(2);
    verify(ctx, times(2)).setResponseType(MediaType.json);

    assertEquals(writer, ctx.responseWriter());

    ctx.responseWriter(w -> w.print("test"));
    verify(writer).print("test");

    ctx.responseWriter(MediaType.text, w -> w.print("test2"));
    verify(writer).print("test2");
  }

  @Test
  void sendRedirect() {
    doReturn(ctx).when(ctx).setResponseHeader(anyString(), anyString());
    doReturn(ctx).when(ctx).send(any(StatusCode.class));

    ctx.sendRedirect("/new-path");
    verify(ctx).setResponseHeader("location", "/new-path");
    verify(ctx).send(StatusCode.FOUND);
  }

  @Test
  void sendOverloads() {
    doReturn(ctx).when(ctx).send(any(ByteBuffer[].class));
    ctx.send(new byte[] {1, 2}, new byte[] {3, 4});
    verify(ctx).send(any(ByteBuffer[].class));

    doReturn(ctx).when(ctx).send(any(String.class), eq(StandardCharsets.UTF_8));
    ctx.send("content");
    verify(ctx).send("content", StandardCharsets.UTF_8);
  }

  @Test
  void sendFileDownloadStream() throws Exception {
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
  void sendFileDownloadFileInputStream() throws Exception {
    FileDownload fd = mock(FileDownload.class);
    when(fd.getContentDisposition()).thenReturn("attachment; filename=test.txt");
    when(fd.getFileSize()).thenReturn(-1L);
    when(fd.getContentType()).thenReturn(MediaType.text);

    FileInputStream fis = mock(FileInputStream.class);
    FileChannel channel = mock(FileChannel.class);
    when(fis.getChannel()).thenReturn(channel);
    when(fd.stream()).thenReturn(fis);

    doReturn(ctx).when(ctx).send(any(FileChannel.class));
    doReturn(ctx).when(ctx).setDefaultResponseType(MediaType.text);

    ctx.send(fd);
    verify(ctx).send(channel);
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
  void sendPathException() {
    Path invalidPath = Path.of("/does/not/exist/12345");
    assertThrows(NoSuchFileException.class, () -> ctx.send(invalidPath));
  }

  // --- Error Handling ---

  @Test
  void sendErrorResponseStarted() {
    Logger log = mock(Logger.class);
    when(router.getLog()).thenReturn(log);
    doReturn(true).when(ctx).isResponseStarted();

    Throwable cause = new IllegalArgumentException("Test");
    when(router.errorCode(cause)).thenReturn(StatusCode.BAD_REQUEST);

    ctx.sendError(cause);
    verify(log).error(anyString(), eq(cause));
  }

  @Test
  void sendErrorNotStarted() {
    Logger log = mock(Logger.class);
    ErrorHandler errorHandler = mock(ErrorHandler.class);
    when(router.getLog()).thenReturn(log);
    when(router.getErrorHandler()).thenReturn(errorHandler);

    doReturn(false).when(ctx).isResponseStarted();
    doReturn(true).when(ctx).getResetHeadersOnError();

    Throwable cause = new IllegalArgumentException("Test Error");
    when(router.errorCode(cause)).thenReturn(StatusCode.SERVER_ERROR);

    ctx.sendError(cause);

    verify(ctx).removeResponseHeaders();
    verify(ctx).setResponseCode(StatusCode.SERVER_ERROR);
    verify(errorHandler).apply(eq(ctx), eq(cause), eq(StatusCode.SERVER_ERROR));
  }

  @Test
  void sendErrorCustomHandlerException() {
    Logger log = mock(Logger.class);
    ErrorHandler errorHandler = mock(ErrorHandler.class);
    when(router.getLog()).thenReturn(log);
    when(router.getErrorHandler()).thenReturn(errorHandler);

    doReturn(false).when(ctx).isResponseStarted();
    doReturn(false).when(ctx).getResetHeadersOnError();
    doReturn(mockMissingValue()).when(ctx).header("Accept");

    doReturn(ctx).when(ctx).setResponseType(any(MediaType.class));
    doReturn(ctx).when(ctx).setResponseCode(any());

    Throwable cause = new IllegalArgumentException("Original Error");
    when(router.errorCode(cause)).thenReturn(StatusCode.SERVER_ERROR);

    // Custom error handler crashes
    doThrow(new RuntimeException("Handler crashed")).when(errorHandler).apply(any(), any(), any());

    ctx.sendError(cause);
    verify(log).error(anyString(), anyString(), any(RuntimeException.class));
  }

  @Test
  void sendErrorConnectionLost() {
    Logger log = mock(Logger.class);
    ErrorHandler errorHandler = mock(ErrorHandler.class);
    when(router.getLog()).thenReturn(log);
    when(router.getErrorHandler()).thenReturn(errorHandler);

    doReturn(false).when(ctx).isResponseStarted();
    doReturn(mockMissingValue()).when(ctx).header("Accept");

    doReturn(ctx).when(ctx).setResponseType(any(MediaType.class));
    doReturn(ctx).when(ctx).setResponseCode(any());

    Throwable cause = new IllegalArgumentException("Original Error");
    when(router.errorCode(cause)).thenReturn(StatusCode.SERVER_ERROR);

    RuntimeException handlerCrash = new RuntimeException("Simulated Connection Lost");
    doThrow(handlerCrash).when(errorHandler).apply(any(), any(), any());

    try (org.mockito.MockedStatic<Server> serverMock =
        mockStatic(Server.class, CALLS_REAL_METHODS)) {
      serverMock.when(() -> Server.connectionLost(handlerCrash)).thenReturn(true);

      ctx.sendError(cause);

      verify(log).debug(anyString(), anyString(), eq(handlerCrash));
    }
  }

  @Test
  void sendErrorFatal() {
    Logger log = mock(Logger.class);
    ErrorHandler errorHandler = mock(ErrorHandler.class);
    when(router.getLog()).thenReturn(log);
    when(router.getErrorHandler()).thenReturn(errorHandler);
    doReturn(false).when(ctx).isResponseStarted();

    OutOfMemoryError fatal = new OutOfMemoryError("Fatal");
    when(router.errorCode(fatal)).thenReturn(StatusCode.SERVER_ERROR);

    assertThrows(OutOfMemoryError.class, () -> ctx.sendError(fatal));
  }

  // --- Output Factory ---

  @Test
  void getOutputFactory() {
    OutputFactory routerFactory = mock(OutputFactory.class);
    OutputFactory contextFactory = mock(OutputFactory.class);
    when(router.getOutputFactory()).thenReturn(routerFactory);
    when(routerFactory.getContextFactory()).thenReturn(contextFactory);

    assertEquals(contextFactory, ctx.getOutputFactory());
  }

  private Value mockMissingValue() {
    Value val = mock(Value.class);
    lenient().when(val.isMissing()).thenReturn(true);
    return val;
  }
}
