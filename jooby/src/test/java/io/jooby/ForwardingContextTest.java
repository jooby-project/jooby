/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import io.jooby.value.Value;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ForwardingContextTest {

  @Test
  public void forwardingBody() {
    Body delegate = mock(Body.class);
    ForwardingContext.ForwardingBody f = new ForwardingContext.ForwardingBody(delegate);

    f.value(StandardCharsets.UTF_8);
    verify(delegate).value(StandardCharsets.UTF_8);
    f.bytes();
    verify(delegate).bytes();
    f.isInMemory();
    verify(delegate).isInMemory();
    f.getSize();
    verify(delegate).getSize();
    f.channel();
    verify(delegate).channel();
    f.stream();
    verify(delegate).stream();
    f.toList(String.class);
    verify(delegate).toList(String.class);
    f.toList();
    verify(delegate).toList();
    f.toSet();
    verify(delegate).toSet();
    f.to(String.class);
    verify(delegate).to(String.class);
    f.toNullable(String.class);
    verify(delegate).toNullable(String.class);
    Type type = mock(Type.class);
    f.to(type);
    verify(delegate).to(type);
    f.toNullable(type);
    verify(delegate).toNullable(type);
    f.get(1);
    verify(delegate).get(1);
    f.get("key");
    verify(delegate).get("key");
    f.getOrDefault("key", "def");
    verify(delegate).getOrDefault("key", "def");
    f.size();
    verify(delegate).size();
    f.iterator();
    verify(delegate).iterator();
    f.resolve("expr");
    verify(delegate).resolve("expr");
    f.resolve("expr", true);
    verify(delegate).resolve("expr", true);
    f.resolve("expr", "{", "}");
    verify(delegate).resolve("expr", "{", "}");
    f.resolve("expr", true, "{", "}");
    verify(delegate).resolve("expr", true, "{", "}");
    Consumer consumer = mock(Consumer.class);
    f.forEach(consumer);
    verify(delegate).forEach(consumer);
    f.spliterator();
    verify(delegate).spliterator();
    f.longValue();
    verify(delegate).longValue();
    f.longValue(1L);
    verify(delegate).longValue(1L);
    f.intValue();
    verify(delegate).intValue();
    f.intValue(1);
    verify(delegate).intValue(1);
    f.byteValue();
    verify(delegate).byteValue();
    f.byteValue((byte) 1);
    verify(delegate).byteValue((byte) 1);
    f.floatValue();
    verify(delegate).floatValue();
    f.floatValue(1f);
    verify(delegate).floatValue(1f);
    f.doubleValue();
    verify(delegate).doubleValue();
    f.doubleValue(1d);
    verify(delegate).doubleValue(1d);
    f.booleanValue();
    verify(delegate).booleanValue();
    f.booleanValue(true);
    verify(delegate).booleanValue(true);
    f.value("def");
    verify(delegate).value("def");
    f.valueOrNull();
    verify(delegate).valueOrNull();
    SneakyThrows.Function sneakyFn = mock(SneakyThrows.Function.class);
    f.value(sneakyFn);
    verify(delegate).value(sneakyFn);
    f.value();
    verify(delegate).value();
    f.toEnum(sneakyFn);
    verify(delegate).toEnum(sneakyFn);
    Function fn = mock(Function.class);
    f.toEnum(sneakyFn, fn);
    verify(delegate).toEnum(sneakyFn, fn);
    f.toOptional();
    verify(delegate).toOptional();
    f.isSingle();
    verify(delegate).isSingle();
    f.isMissing();
    verify(delegate).isMissing();
    f.isPresent();
    verify(delegate).isPresent();
    f.isArray();
    verify(delegate).isArray();
    f.isObject();
    verify(delegate).isObject();
    f.name();
    verify(delegate).name();
    f.toOptional(String.class);
    verify(delegate).toOptional(String.class);
    f.toSet(String.class);
    verify(delegate).toSet(String.class);
    f.toMultimap();
    verify(delegate).toMultimap();
    f.toMap();
    verify(delegate).toMap();
  }

  @Test
  public void forwardingValue() {
    Value delegate = mock(Value.class);
    ForwardingContext.ForwardingValue f = new ForwardingContext.ForwardingValue(delegate);

    f.get(1);
    verify(delegate).get(1);
    f.get("key");
    verify(delegate).get("key");
    f.getOrDefault("key", "def");
    verify(delegate).getOrDefault("key", "def");
    f.size();
    verify(delegate).size();
    f.iterator();
    verify(delegate).iterator();
    f.resolve("expr");
    verify(delegate).resolve("expr");
    f.resolve("expr", true);
    verify(delegate).resolve("expr", true);
    f.resolve("expr", "{", "}");
    verify(delegate).resolve("expr", "{", "}");
    f.resolve("expr", true, "{", "}");
    verify(delegate).resolve("expr", true, "{", "}");
    Consumer consumer = mock(Consumer.class);
    f.forEach(consumer);
    verify(delegate).forEach(consumer);
    f.spliterator();
    verify(delegate).spliterator();
    f.longValue();
    verify(delegate).longValue();
    f.longValue(1L);
    verify(delegate).longValue(1L);
    f.intValue();
    verify(delegate).intValue();
    f.intValue(1);
    verify(delegate).intValue(1);
    f.byteValue();
    verify(delegate).byteValue();
    f.byteValue((byte) 1);
    verify(delegate).byteValue((byte) 1);
    f.floatValue();
    verify(delegate).floatValue();
    f.floatValue(1f);
    verify(delegate).floatValue(1f);
    f.doubleValue();
    verify(delegate).doubleValue();
    f.doubleValue(1d);
    verify(delegate).doubleValue(1d);
    f.booleanValue();
    verify(delegate).booleanValue();
    f.booleanValue(true);
    verify(delegate).booleanValue(true);
    f.value("def");
    verify(delegate).value("def");
    f.valueOrNull();
    verify(delegate).valueOrNull();
    SneakyThrows.Function sneakyFn = mock(SneakyThrows.Function.class);
    f.value(sneakyFn);
    verify(delegate).value(sneakyFn);
    f.value();
    verify(delegate).value();
    f.toList();
    verify(delegate).toList();
    f.toSet();
    verify(delegate).toSet();
    f.toEnum(sneakyFn);
    verify(delegate).toEnum(sneakyFn);
    Function fn = mock(Function.class);
    f.toEnum(sneakyFn, fn);
    verify(delegate).toEnum(sneakyFn, fn);
    f.toOptional();
    verify(delegate).toOptional();
    f.isSingle();
    verify(delegate).isSingle();
    f.isMissing();
    verify(delegate).isMissing();
    f.isPresent();
    verify(delegate).isPresent();
    f.isArray();
    verify(delegate).isArray();
    f.isObject();
    verify(delegate).isObject();
    f.name();
    verify(delegate).name();
    f.toOptional(String.class);
    verify(delegate).toOptional(String.class);
    f.toList(String.class);
    verify(delegate).toList(String.class);
    f.toSet(String.class);
    verify(delegate).toSet(String.class);
    f.to(String.class);
    verify(delegate).to(String.class);
    f.toNullable(String.class);
    verify(delegate).toNullable(String.class);
    f.toMultimap();
    verify(delegate).toMultimap();
    f.toMap();
    verify(delegate).toMap();
  }

  @Test
  public void forwardingQueryString() {
    QueryString delegate = mock(QueryString.class);
    ForwardingContext.ForwardingQueryString f =
        new ForwardingContext.ForwardingQueryString(delegate);

    f.toEmpty(String.class);
    verify(delegate).toEmpty(String.class);
    f.queryString();
    verify(delegate).queryString();
  }

  @Test
  public void forwardingFormdata() {
    Formdata delegate = mock(Formdata.class);
    ForwardingContext.ForwardingFormdata f = new ForwardingContext.ForwardingFormdata(delegate);

    Value val = mock(Value.class);
    f.put("path", val);
    verify(delegate).put("path", val);
    f.put("path", "value");
    verify(delegate).put("path", "value");
    Collection<String> vals = List.of("v");
    f.put("path", vals);
    verify(delegate).put("path", vals);
    FileUpload file = mock(FileUpload.class);
    f.put("name", file);
    verify(delegate).put("name", file);
    f.files();
    verify(delegate).files();
    f.files("name");
    verify(delegate).files("name");
    f.file("name");
    verify(delegate).file("name");
  }

  @Test
  public void forwardingContextProperties() throws Exception {
    Context delegate = mock(Context.class);
    ForwardingContext f = new ForwardingContext(delegate);

    assertSame(delegate, f.getDelegate());

    f.getUser();
    verify(delegate).getUser();
    assertSame(f, f.setUser("user"));
    verify(delegate).setUser("user");

    when(delegate.forward("/path")).thenReturn("Result");
    assertEquals("Result", f.forward("/path"));

    Context nestedCtx = mock(Context.class);
    when(delegate.forward("/nested")).thenReturn(nestedCtx);
    assertSame(f, f.forward("/nested"));

    f.matches("pattern");
    verify(delegate).matches("pattern");
    f.isSecure();
    verify(delegate).isSecure();
    f.getAttributes();
    verify(delegate).getAttributes();
    f.getAttribute("key");
    verify(delegate).getAttribute("key");
    assertSame(f, f.setAttribute("key", "val"));
    verify(delegate).setAttribute("key", "val");
    f.getRouter();
    verify(delegate).getRouter();

    OutputFactory outFactory = mock(OutputFactory.class);
    when(delegate.getOutputFactory()).thenReturn(outFactory);
    when(outFactory.getContextFactory()).thenReturn(outFactory);
    assertSame(outFactory, f.getOutputFactory());

    f.flash();
    verify(delegate).flash();
    f.flashOrNull();
    verify(delegate).flashOrNull();
    f.flash("n");
    verify(delegate).flash("n");
    f.flash("n", "def");
    verify(delegate).flash("n", "def");

    f.session("n");
    verify(delegate).session("n");
    f.session("n", "def");
    verify(delegate).session("n", "def");
    f.session();
    verify(delegate).session();
    f.sessionOrNull();
    verify(delegate).sessionOrNull();

    f.cookie("n");
    verify(delegate).cookie("n");
    f.cookie("n", "def");
    verify(delegate).cookie("n", "def");
    f.cookieMap();
    verify(delegate).cookieMap();

    f.getMethod();
    verify(delegate).getMethod();
    assertSame(f, f.setMethod("GET"));
    verify(delegate).setMethod("GET");
    f.getRoute();
    verify(delegate).getRoute();

    // Note: setRoute returns the delegate's return directly.
    Route route = mock(Route.class);
    when(delegate.setRoute(route)).thenReturn(delegate);
    assertSame(delegate, f.setRoute(route));
    verify(delegate).setRoute(route);

    f.getRequestPath();
    verify(delegate).getRequestPath();
    assertSame(f, f.setRequestPath("/p"));
    verify(delegate).setRequestPath("/p");

    f.lookup();
    verify(delegate).lookup();
    ParamSource source = ParamSource.PATH;
    f.lookup("n", source);
    verify(delegate).lookup("n", source);

    f.path("n");
    verify(delegate).path("n");
    f.path(String.class);
    verify(delegate).path(String.class);
    f.path();
    verify(delegate).path();
    f.pathMap();
    verify(delegate).pathMap();
    Map<String, String> map = Map.of("k", "v");
    assertSame(f, f.setPathMap(map));
    verify(delegate).setPathMap(map);

    f.query();
    verify(delegate).query();
    f.query("n");
    verify(delegate).query("n");
    f.query("n", "def");
    verify(delegate).query("n", "def");
    f.queryString();
    verify(delegate).queryString();
    f.query(String.class);
    verify(delegate).query(String.class);
    f.queryMap();
    verify(delegate).queryMap();

    f.header();
    verify(delegate).header();
    f.header("n");
    verify(delegate).header("n");
    f.header("n", "def");
    verify(delegate).header("n", "def");
    f.headerMap();
    verify(delegate).headerMap();

    f.accept(MediaType.json);
    verify(delegate).accept(MediaType.json);
    List<MediaType> mediaTypes = List.of(MediaType.json);
    f.accept(mediaTypes);
    verify(delegate).accept(mediaTypes);

    f.getRequestType();
    verify(delegate).getRequestType();
    f.getRequestType(MediaType.json);
    verify(delegate).getRequestType(MediaType.json);
    f.getRequestLength();
    verify(delegate).getRequestLength();
    f.getRemoteAddress();
    verify(delegate).getRemoteAddress();
    assertSame(f, f.setRemoteAddress("127.0.0.1"));
    verify(delegate).setRemoteAddress("127.0.0.1");

    f.getHost();
    verify(delegate).getHost();
    assertSame(f, f.setHost("host"));
    verify(delegate).setHost("host");
    f.getServerPort();
    verify(delegate).getServerPort();
    f.getServerHost();
    verify(delegate).getServerHost();
    f.getPort();
    verify(delegate).getPort();
    assertSame(f, f.setPort(80));
    verify(delegate).setPort(80);
    f.getHostAndPort();
    verify(delegate).getHostAndPort();
    f.getRequestURL();
    verify(delegate).getRequestURL();
    f.getRequestURL("p");
    verify(delegate).getRequestURL("p");
    f.getProtocol();
    verify(delegate).getProtocol();
    f.getClientCertificates();
    verify(delegate).getClientCertificates();
    f.getScheme();
    verify(delegate).getScheme();
    assertSame(f, f.setScheme("http"));
    verify(delegate).setScheme("http");

    f.form();
    verify(delegate).form();
    f.form("n");
    verify(delegate).form("n");
    f.form("n", "def");
    verify(delegate).form("n", "def");
    f.form(String.class);
    verify(delegate).form(String.class);
    f.formMap();
    verify(delegate).formMap();

    f.files();
    verify(delegate).files();
    f.files("n");
    verify(delegate).files("n");
    f.file("n");
    verify(delegate).file("n");

    f.body();
    verify(delegate).body();
    f.body(String.class);
    verify(delegate).body(String.class);
    Type type = mock(Type.class);
    f.body(type);
    verify(delegate).body(type);

    f.getValueFactory();
    verify(delegate).getValueFactory();
    f.decode(type, MediaType.json);
    verify(delegate).decode(type, MediaType.json);
    f.decoder(MediaType.json);
    verify(delegate).decoder(MediaType.json);

    f.isInIoThread();
    verify(delegate).isInIoThread();
    Runnable runnable = mock(Runnable.class);
    assertSame(f, f.dispatch(runnable));
    verify(delegate).dispatch(runnable);
    Executor executor = mock(Executor.class);
    assertSame(f, f.dispatch(executor, runnable));
    verify(delegate).dispatch(executor, runnable);

    WebSocket.Initializer wsInit = mock(WebSocket.Initializer.class);
    assertSame(f, f.upgrade(wsInit));
    verify(delegate).upgrade(wsInit);
    ServerSentEmitter.Handler sseHandler = mock(ServerSentEmitter.Handler.class);
    assertSame(f, f.upgrade(sseHandler));
    verify(delegate).upgrade(sseHandler);

    Date date = new Date();
    assertSame(f, f.setResponseHeader("n", date));
    verify(delegate).setResponseHeader("n", date);
    Instant instant = Instant.now();
    assertSame(f, f.setResponseHeader("n", instant));
    verify(delegate).setResponseHeader("n", instant);
    Object obj = new Object();
    assertSame(f, f.setResponseHeader("n", obj));
    verify(delegate).setResponseHeader("n", obj);
    assertSame(f, f.setResponseHeader("n", "v"));
    verify(delegate).setResponseHeader("n", "v");
    assertSame(f, f.removeResponseHeader("n"));
    verify(delegate).removeResponseHeader("n");
    assertSame(f, f.removeResponseHeaders());
    verify(delegate).removeResponseHeaders();
    f.getResponseHeader("n");
    verify(delegate).getResponseHeader("n");

    f.getResponseLength();
    verify(delegate).getResponseLength();
    assertSame(f, f.setResponseLength(10L));
    verify(delegate).setResponseLength(10L);

    Cookie cookie = mock(Cookie.class);
    assertSame(f, f.setResponseCookie(cookie));
    verify(delegate).setResponseCookie(cookie);

    assertSame(f, f.setResponseType("type"));
    verify(delegate).setResponseType("type");
    assertSame(f, f.setResponseType(MediaType.json));
    verify(delegate).setResponseType(MediaType.json);

    // Fix: Using a different MediaType here prevents Mockito's TooManyActualInvocations error
    // since both methods delegate to ctx.setResponseType()
    assertSame(f, f.setDefaultResponseType(MediaType.html));
    verify(delegate).setResponseType(MediaType.html);
    f.getResponseType();
    verify(delegate).getResponseType();

    assertSame(f, f.setResponseCode(StatusCode.OK));
    verify(delegate).setResponseCode(StatusCode.OK);
    assertSame(f, f.setResponseCode(200));
    verify(delegate).setResponseCode(200);
    f.getResponseCode();
    verify(delegate).getResponseCode();

    assertSame(f, f.render(obj));
    verify(delegate).render(obj);

    f.responseStream();
    verify(delegate).responseStream();
    f.responseStream(MediaType.json);
    verify(delegate).responseStream(MediaType.json);

    SneakyThrows.Consumer outConsumer = mock(SneakyThrows.Consumer.class);
    when(delegate.responseStream(MediaType.json, outConsumer)).thenReturn(delegate);
    assertSame(delegate, f.responseStream(MediaType.json, outConsumer));
    verify(delegate).responseStream(MediaType.json, outConsumer);

    when(delegate.responseStream(outConsumer)).thenReturn(delegate);
    assertSame(delegate, f.responseStream(outConsumer));
    verify(delegate).responseStream(outConsumer);

    f.responseSender();
    verify(delegate).responseSender();
    f.responseWriter();
    verify(delegate).responseWriter();
    f.responseWriter(MediaType.json);
    verify(delegate).responseWriter(MediaType.json);

    SneakyThrows.Consumer writerConsumer = mock(SneakyThrows.Consumer.class);
    when(delegate.responseWriter(writerConsumer)).thenReturn(delegate);
    assertSame(delegate, f.responseWriter(writerConsumer));
    verify(delegate).responseWriter(writerConsumer);

    when(delegate.responseWriter(MediaType.json, writerConsumer)).thenReturn(delegate);
    assertSame(delegate, f.responseWriter(MediaType.json, writerConsumer));
    verify(delegate).responseWriter(MediaType.json, writerConsumer);

    assertSame(f, f.sendRedirect("loc"));
    verify(delegate).sendRedirect("loc");
    assertSame(f, f.sendRedirect(StatusCode.FOUND, "loc"));
    verify(delegate).sendRedirect(StatusCode.FOUND, "loc");

    assertSame(f, f.send("data"));
    verify(delegate).send("data");
    assertSame(f, f.send("data", StandardCharsets.UTF_8));
    verify(delegate).send("data", StandardCharsets.UTF_8);
    byte[] bytes = new byte[0];
    assertSame(f, f.send(bytes));
    verify(delegate).send(bytes);
    ByteBuffer buffer = ByteBuffer.allocate(0);
    assertSame(f, f.send(buffer));
    verify(delegate).send(buffer);
    Output output = mock(Output.class);
    assertSame(f, f.send(output));
    verify(delegate).send(output);
    byte[][] bytesArr = new byte[][] {bytes};
    assertSame(f, f.send(bytesArr));
    verify(delegate).send(bytesArr);
    ByteBuffer[] buffArr = new ByteBuffer[] {buffer};
    assertSame(f, f.send(buffArr));
    verify(delegate).send(buffArr);
    ReadableByteChannel rbChannel = mock(ReadableByteChannel.class);
    assertSame(f, f.send(rbChannel));
    verify(delegate).send(rbChannel);
    InputStream is = mock(InputStream.class);
    assertSame(f, f.send(is));
    verify(delegate).send(is);
    FileDownload fd = mock(FileDownload.class);
    assertSame(f, f.send(fd));
    verify(delegate).send(fd);
    Path p = Paths.get("");
    assertSame(f, f.send(p));
    verify(delegate).send(p);
    FileChannel fc = mock(FileChannel.class);
    assertSame(f, f.send(fc));
    verify(delegate).send(fc);
    assertSame(f, f.send(StatusCode.OK));
    verify(delegate).send(StatusCode.OK);

    Throwable cause = new Exception();
    assertSame(f, f.sendError(cause));
    verify(delegate).sendError(cause);
    assertSame(f, f.sendError(cause, StatusCode.BAD_REQUEST));
    verify(delegate).sendError(cause, StatusCode.BAD_REQUEST);

    f.isResponseStarted();
    verify(delegate).isResponseStarted();
    f.getResetHeadersOnError();
    verify(delegate).getResetHeadersOnError();
    assertSame(f, f.setResetHeadersOnError(true));
    verify(delegate).setResetHeadersOnError(true);

    Route.Complete onCompleteTask = mock(Route.Complete.class);
    assertSame(f, f.onComplete(onCompleteTask));
    verify(delegate).onComplete(onCompleteTask);

    f.require(String.class);
    verify(delegate).require(String.class);
    f.require(String.class, "n");
    verify(delegate).require(String.class, "n");
    Reified reified = mock(Reified.class);
    f.require(reified);
    verify(delegate).require(reified);
    f.require(reified, "n");
    verify(delegate).require(reified, "n");
    ServiceKey srvKey = mock(ServiceKey.class);
    f.require(srvKey);
    verify(delegate).require(srvKey);
  }
}
