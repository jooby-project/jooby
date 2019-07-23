package io.jooby.compiler;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.MockContext;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Reified;
import io.jooby.Session;
import io.jooby.StatusCode;
import org.junit.jupiter.api.Test;
import source.CustomGenericType;
import source.EnumParam;
import source.JavaBeanParam;
import source.Provisioning;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MvcProcessorTest {

  @Test
  public void typeInjection() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("sessionOrNull", args(Optional.class), handler -> {
          assertEquals("session:false", handler.apply(new MockContext()));
        })
        .compile("noarg", args(), handler -> {
          assertEquals("noarg", handler.apply(new MockContext()));
        })
        .compile("context", args(Context.class), handler -> {
          assertEquals("ctx", handler.apply(new MockContext()));
        })
        .compile("queryString", args(QueryString.class), handler -> {
          assertEquals("queryString", handler.apply(new MockContext()));
        })
        .compile("formdata", args(Formdata.class), handler -> {
          assertEquals("formdata", handler.apply(new MockContext()));
        })
        .compile("multipart", args(Multipart.class), handler -> {
          assertEquals("multipart", handler.apply(new MockContext()));
        })
        .compile("contextFirst", args(Context.class, QueryString.class), handler -> {
          assertEquals("ctxfirst", handler.apply(new MockContext()));
        })
        .compile("flashMap", args(FlashMap.class), handler -> {
          assertEquals("flashMap", handler.apply(new MockContext()));
        })

        .compile("session", args(Session.class), handler -> {
          assertEquals("session", handler.apply(new MockContext()));
        });
  }

  @Test
  public void queryParam() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("queryParam", args(String.class), handler -> {
          assertEquals("search", handler.apply(new MockContext().setPathString("/?q=search")));
        })
    ;
  }

  @Test
  public void cookieParam() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("cookieParam", args(String.class), handler -> {
          assertEquals("cookie",
              handler.apply(new MockContext().setCookieMap(mapOf("c", "cookie"))));
        })
    ;
  }

  @Test
  public void headerParam() throws Exception {
    long epoc = System.currentTimeMillis();
    new TestProcessor(new Provisioning())
        .compile("headerParam", args(Instant.class), handler -> {
          assertEquals(String.valueOf(epoc),
              handler.apply(new MockContext().setRequestHeader("instant", String.valueOf(epoc))));
        })
    ;
  }

  @Test
  public void flashParam() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("flashParam", args(String.class), handler -> {
          assertEquals("hey", handler.apply(new MockContext().setFlashAttribute("message", "hey")));
        })
    ;
  }

  @Test
  public void formParam() throws Exception {
    Multipart formdata = Multipart.create();
    formdata.put("name", "yo");
    new TestProcessor(new Provisioning())
        .compile("formParam", args(String.class), handler -> {
          assertEquals("yo", handler.apply(new MockContext().setMultipart(formdata)));
        })
    ;
  }

  @Test
  public void pathParam() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("pathParam", args(String.class), handler -> {
          assertEquals("v1", handler.apply(new MockContext().setPathMap(mapOf("p1", "v1"))));
        })
        .compile("bytePathParam", args(byte.class), handler -> {
          assertEquals("2", handler.apply(new MockContext().setPathMap(mapOf("p1", "2"))));
        })
        .compile("longPathParam", args(long.class), handler -> {
          assertEquals("3", handler.apply(new MockContext().setPathMap(mapOf("p1", "3"))));
        })
        .compile("floatPathParam", args(float.class), handler -> {
          assertEquals("3.5", handler.apply(new MockContext().setPathMap(mapOf("p1", "3.5"))));
        })
        .compile("doublePathParam", args(double.class), handler -> {
          assertEquals("3.55", handler.apply(new MockContext().setPathMap(mapOf("p1", "3.55"))));
        })
        .compile("booleanPathParam", args(boolean.class), handler -> {
          assertEquals("true", handler.apply(new MockContext().setPathMap(mapOf("p1", "true"))));
        })
        .compile("intPathParam", args(int.class), handler -> {
          assertEquals("1", handler.apply(new MockContext().setPathMap(mapOf("p1", "1"))));
        })
        .compile("optionalStringPathParam", args(Optional.class), true, handler -> {
          assertEquals("Optional[x]",
              handler.apply(new MockContext().setPathMap(mapOf("p1", "x"))));
        })
        .compile("optionalIntPathParam", args(Optional.class), handler -> {
          assertEquals("Optional[7]",
              handler.apply(new MockContext().setPathMap(mapOf("p1", "7"))));
        })
        .compile("javaBeanPathParam", args(JavaBeanParam.class), true, handler -> {
          assertEquals("bar", handler.apply(new MockContext().setPathMap(mapOf("foo", "bar"))));
        })
        .compile("listStringPathParam", args(List.class), handler -> {
          assertEquals("[bar]",
              handler.apply(new MockContext().setPathMap(mapOf("values", "bar"))));
        })
        .compile("listDoublePathParam", args(List.class), handler -> {
          assertEquals("[6.7]",
              handler.apply(new MockContext().setPathMap(mapOf("values", "6.7"))));
        })
        .compile("listBeanPathParam", args(List.class), handler -> {
          assertEquals("[bar]", handler.apply(new MockContext().setPathMap(mapOf("foo", "bar"))));
        })
        .compile("setStringPathParam", args(Set.class), handler -> {
          assertEquals("[bar]",
              handler.apply(new MockContext().setPathMap(mapOf("values", "bar"))));
        })
        .compile("setDoublePathParam", args(Set.class), handler -> {
          assertEquals("[6.7]",
              handler.apply(new MockContext().setPathMap(mapOf("values", "6.7"))));
        })
        .compile("setBeanPathParam", args(Set.class), handler -> {
          assertEquals("[bar]", handler.apply(new MockContext().setPathMap(mapOf("foo", "bar"))));
        })
        .compile("enumParam", args(EnumParam.class), handler -> {
          assertEquals("A", handler.apply(new MockContext().setPathMap(mapOf("letter", "a"))));
        })
        .compile("optionalEnumParam", args(Optional.class), handler -> {
          assertEquals("Optional[A]",
              handler.apply(new MockContext().setPathMap(mapOf("letter", "a"))));
        })
        .compile("listEnumParam", args(List.class), handler -> {
          assertEquals("[B]", handler.apply(new MockContext().setPathMap(mapOf("letter", "B"))));
        })
        .compile("primitiveWrapper", args(Integer.class), handler -> {
          assertEquals("null", handler.apply(new MockContext()));
        })
        .compile("primitiveWrapper", args(Integer.class), handler -> {
          assertEquals("9", handler.apply(new MockContext().setPathMap(mapOf("value", "9"))));
        })
    ;
  }

  @Test
  public void multipleParameters() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("parameters", args(String.class, Context.class, int.class, JavaBeanParam.class),
            handler -> {
              assertEquals("123GET /1x", handler.apply(
                  new MockContext().setPathMap(mapOf("path", "123"))
                      .setPathString("/?offset=1&foo=x")));
            })
    ;
  }

  @Test
  public void fileParam() throws Exception {
    FileUpload file = mock(FileUpload.class);
    Multipart multipart = Multipart.create();
    multipart.put("file", file);
    new TestProcessor(new Provisioning())
        .compile("fileParam", args(FileUpload.class), handler -> {
          assertEquals(file.toString(), handler.apply(new MockContext().setMultipart(multipart)));
        })
        .compile("fileParams", args(List.class), handler -> {
          assertEquals(Arrays.asList(file).toString(),
              handler.apply(new MockContext().setMultipart(multipart)));
        })
    ;
  }

  @Test
  public void specialParam() throws Exception {
    UUID uuid = UUID.randomUUID();
    BigDecimal bigDecimal = new BigDecimal("88.6");
    BigInteger bigInteger = new BigInteger("888888");
    Charset charset = StandardCharsets.UTF_8;

    new TestProcessor(new Provisioning())
        .compile("uuidParam", args(UUID.class), handler -> {
          assertEquals(uuid.toString(),
              handler.apply(new MockContext().setPathString("/?value=" + uuid.toString())));
        })
        .compile("bigDecimalParam", args(BigDecimal.class), handler -> {
          assertEquals(bigDecimal.toString(),
              handler.apply(new MockContext().setPathString("/?value=" + bigDecimal.toString())));
        })
        .compile("bigIntegerParam", args(BigInteger.class), handler -> {
          assertEquals(bigInteger.toString(),
              handler.apply(new MockContext().setPathString("/?value=" + bigInteger.toString())));
        })
        .compile("charsetParam", args(Charset.class), handler -> {
          assertEquals(charset.toString(),
              handler.apply(new MockContext().setPathString("/?value=" + charset.toString())));
        })
        .compile("pathParam", args(Path.class), handler -> {
          Path path = mock(Path.class);
          FileUpload file = mock(FileUpload.class);
          when(file.path()).thenReturn(path);
          Multipart multipart = Multipart.create();
          multipart.put("file", file);
          assertEquals(path.toString(),
              handler.apply(new MockContext().setMultipart(multipart)));
        })
    ;
  }

  @Test
  public void returnTypes() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("returnByte", args(), handler -> {
          assertEquals(Byte.valueOf((byte) 8), handler.apply(new MockContext()));
        })
        .compile("returnShort", args(), handler -> {
          assertEquals(Short.valueOf((short) 8), handler.apply(new MockContext()));
        })
        .compile("returnInteger", args(), handler -> {
          assertEquals(Integer.valueOf(7), handler.apply(new MockContext()));
        })
        .compile("returnLong", args(), handler -> {
          assertEquals(Long.valueOf(9), handler.apply(new MockContext()));
        })
        .compile("returnFloat", args(), handler -> {
          assertEquals(Float.valueOf(7.9f), handler.apply(new MockContext()));
        })
        .compile("returnDouble", args(), handler -> {
          assertEquals(Double.valueOf(8.9), handler.apply(new MockContext()));
        })
        .compile("returnChar", args(), handler -> {
          assertEquals(Character.valueOf('c'), handler.apply(new MockContext()));
        })
        .compile("returnStatusCode", args(), handler -> {
          MockContext ctx = new MockContext();
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.NO_CONTENT, ctx.getResponseCode());
        })
        .compile("statusCode", args(StatusCode.class, String.class), handler -> {
          MockContext ctx = new MockContext().setPathString("/?statusCode=200&q=*:*");
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.OK, ctx.getResponseCode());
        })
        .compile("noContent", args(), handler -> {
          MockContext ctx = new MockContext();
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.NO_CONTENT, ctx.getResponseCode());
        })
        .compile("sideEffect", args(Context.class), handler -> {
          MockContext ctx = new MockContext();
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.CREATED, ctx.getResponseCode());
        })
    ;
  }

  @Test
  public void body() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("POST", "bodyMapParam", args(Map.class), handler -> {
          Map map = mock(Map.class);
          Body body = mock(Body.class);
          when(body.to(Reified.map(String.class, Object.class))).thenReturn(map);
          assertEquals(map, handler.apply(new MockContext().setBody(body)));
        })
        .compile("POST", "bodyStringParam", args(String.class), handler -> {
          assertEquals("...", handler.apply(new MockContext().setBody("...")));
        })
        .compile("POST", "bodyBytesParam", args(byte[].class), handler -> {
          assertEquals("...",
              handler.apply(new MockContext().setBody("...".getBytes(StandardCharsets.UTF_8))));
        })
        .compile("POST", "bodyInputStreamParam", args(InputStream.class), handler -> {
          InputStream stream = mock(InputStream.class);
          Body body = mock(Body.class);
          when(body.stream()).thenReturn(stream);
          assertEquals(stream.toString(), handler.apply(new MockContext().setBody(body)));
        })
        .compile("POST", "bodyChannelParam", args(ReadableByteChannel.class), handler -> {
          ReadableByteChannel channel = mock(ReadableByteChannel.class);
          Body body = mock(Body.class);
          when(body.channel()).thenReturn(channel);
          assertEquals(channel.toString(), handler.apply(new MockContext().setBody(body)));
        })
        .compile("POST", "bodyBeanParam", args(JavaBeanParam.class), handler -> {
          JavaBeanParam bean = mock(JavaBeanParam.class);
          Body body = mock(Body.class);
          when(body.to(JavaBeanParam.class)).thenReturn(bean);
          assertEquals(bean.toString(), handler.apply(new MockContext().setBody(body)));
        })
        .compile("POST", "bodyIntParam", args(int.class), handler -> {
          Body body = mock(Body.class);
          when(body.intValue()).thenReturn(9);
          assertEquals(9, handler.apply(new MockContext().setBody(body)));
        })
        .compile("POST", "bodyOptionalIntParam", args(Optional.class), handler -> {
          Body body = mock(Body.class);
          when(body.toOptional(Integer.class)).thenReturn(Optional.of(9));
          assertEquals(Optional.of(9), handler.apply(new MockContext().setBody(body)));
        })
        .compile("POST", "bodyCustomGenericParam", args(CustomGenericType.class), handler -> {
          CustomGenericType generic = new CustomGenericType<>();
          Body body = mock(Body.class);
          Reified parameterized = Reified
              .getParameterized(CustomGenericType.class, String.class);
          when(body.to(parameterized)).thenReturn(generic);
          assertEquals(generic, handler.apply(new MockContext().setBody(body)));
        })
    ;
  }

  public static Class[] args(Class... args) {
    return args;
  }

  private Map<String, String> mapOf(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }
}
