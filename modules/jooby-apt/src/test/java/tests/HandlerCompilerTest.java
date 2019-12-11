package tests;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.MissingValueException;
import io.jooby.MockContext;
import io.jooby.Multipart;
import io.jooby.ProvisioningException;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.apt.MvcHandlerCompilerRunner;
import io.jooby.internal.MockContextHelper;
import org.junit.jupiter.api.Test;
import source.CustomGenericType;
import source.JavaBeanParam;
import source.JaxrsController;
import source.NoPathRoute;
import source.NullRoutes;
import source.Provisioning;
import source.RouteContextPath;
import source.RouteInjection;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HandlerCompilerTest {

  @Test
  public void typeInjection() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/noarg", handler -> {
          assertEquals("noarg", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/context", handler -> {
          assertEquals("ctx", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/queryString", handler -> {
          assertEquals("queryString", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/formdata", handler -> {
          assertEquals("formdata", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/multipart", handler -> {
          assertEquals("multipart", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/contextFirst", handler -> {
          assertEquals("ctxfirst", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/flashMap", handler -> {
          assertEquals("flashMap", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/sessionOrNull", handler -> {
          assertEquals("session:false", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/session", handler -> {
          assertEquals("session", handler.apply(MockContextHelper.mockContext()));
        });
  }

  @Test
  public void queryParam() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/queryParam", handler -> {
          assertEquals("search",
              handler.apply(MockContextHelper.mockContext().setPathString("/?q=search")));
        })
    ;
  }

  @Test
  public void cookieParam() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/cookieParam", handler -> {
          assertEquals("cookie",
              handler.apply(MockContextHelper.mockContext().setCookieMap(mapOf("c", "cookie"))));
        })
    ;
  }

  @Test
  public void headerParam() throws Exception {
    long epoc = System.currentTimeMillis();
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/headerParam", handler -> {
          assertEquals(String.valueOf(epoc),
              handler.apply(MockContextHelper.mockContext()
                  .setRequestHeader("instant", String.valueOf(epoc))));
        })
    ;
  }

  @Test
  public void flashParam() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/flashParam", handler -> {
          assertEquals("hey",
              handler.apply(MockContextHelper.mockContext().setFlashAttribute("message", "hey")));
        })
    ;
  }

  @Test
  public void formParam() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/formParam", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          Multipart formdata = Multipart.create(ctx);
          formdata.put("name", "yo");
          assertEquals("yo", handler.apply(ctx.setMultipart(formdata)));
        })
    ;
  }

  @Test
  public void pathParam() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/listBeanPathParam", handler -> {
          assertEquals("[bar]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("foo", "bar"))));
        })
        .compile("/p/pathParam", handler -> {
          assertEquals("v1",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "v1"))));
        })
        .compile("/p/bytePathParam", handler -> {
          assertEquals("2",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "2"))));
        })
        .compile("/p/longPathParam", handler -> {
          assertEquals("3",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "3"))));
        })
        .compile("/p/floatPathParam", handler -> {
          assertEquals("3.5",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "3.5"))));
        })
        .compile("/p/doublePathParam", handler -> {
          assertEquals("3.55",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "3.55"))));
        })
        .compile("/p/booleanPathParam", handler -> {
          assertEquals("true",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "true"))));
        })
        .compile("/p/intPathParam", handler -> {
          assertEquals("1",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "1"))));
        })
        .compile("/p/optionalStringPathParam", handler -> {
          assertEquals("Optional[x]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "x"))));
        })
        .compile("/p/optionalIntPathParam", handler -> {
          assertEquals("Optional[7]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("p1", "7"))));
        })
        .compile("/p/javaBeanPathParam", handler -> {
          assertEquals("bar",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("foo", "bar"))));
        })
        .compile("/p/listStringPathParam", handler -> {
          assertEquals("[bar]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("values", "bar"))));
        })
        .compile("/p/listDoublePathParam", handler -> {
          assertEquals("[6.7]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("values", "6.7"))));
        })
        .compile("/p/setStringPathParam", handler -> {
          assertEquals("[bar]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("values", "bar"))));
        })
        .compile("/p/setDoublePathParam", handler -> {
          assertEquals("[6.7]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("values", "6.7"))));
        })
        .compile("/p/setBeanPathParam", handler -> {
          assertEquals("[bar]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("foo", "bar"))));
        })
        .compile("/p/enumParam", handler -> {
          assertEquals("A",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("letter", "a"))));
        })
        .compile("/p/optionalEnumParam", handler -> {
          assertEquals("Optional[A]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("letter", "a"))));
        })
        .compile("/p/listEnumParam", handler -> {
          assertEquals("[B]",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("letter", "B"))));
        })
        .compile("/p/primitiveWrapper", handler -> {
          assertEquals("null", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/primitiveWrapper", handler -> {
          assertEquals("9",
              handler.apply(MockContextHelper.mockContext().setPathMap(mapOf("value", "9"))));
        })
    ;
  }

  @Test
  public void multipleParameters() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/parameters",
            handler -> {
              assertEquals("123GET /1x", handler.apply(
                  MockContextHelper.mockContext().setPathMap(mapOf("path", "123"))
                      .setPathString("/?offset=1&foo=x")));
            })
    ;
  }

  @Test
  public void fileParam() throws Exception {
    FileUpload file = mock(FileUpload.class);
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/fileParam", handler -> {
          assertEquals(file.toString(),
              handler.apply(MockContextHelper.mockContext().setFile("file", file)));
        })
        .compile("/p/fileParams", handler -> {
          assertEquals(Arrays.asList(file).toString(),
              handler.apply(MockContextHelper.mockContext().setFile("file", file)));
        })
    ;
  }

  @Test
  public void specialParam() throws Exception {
    UUID uuid = UUID.randomUUID();
    BigDecimal bigDecimal = new BigDecimal("88.6");
    BigInteger bigInteger = new BigInteger("888888");
    Charset charset = StandardCharsets.UTF_8;

    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/uuidParam", handler -> {
          assertEquals(uuid.toString(),
              handler.apply(
                  MockContextHelper.mockContext().setPathString("/?value=" + uuid.toString())));
        })
        .compile("/p/bigDecimalParam", handler -> {
          assertEquals(bigDecimal.toString(),
              handler.apply(MockContextHelper.mockContext()
                  .setPathString("/?value=" + bigDecimal.toString())));
        })
        .compile("/p/bigIntegerParam", handler -> {
          assertEquals(bigInteger.toString(),
              handler.apply(MockContextHelper.mockContext()
                  .setPathString("/?value=" + bigInteger.toString())));
        })
        .compile("/p/charsetParam", handler -> {
          assertEquals(charset.toString(),
              handler.apply(
                  MockContextHelper.mockContext().setPathString("/?value=" + charset.toString())));
        })
        .compile("/p/pathFormParam", handler -> {
          Path path = mock(Path.class);
          FileUpload file = mock(FileUpload.class);
          when(file.path()).thenReturn(path);
          assertEquals(path.toString(),
              handler.apply(MockContextHelper.mockContext().setFile("file", file)));
        })
    ;
  }

  @Test
  public void returnTypes() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("/p/returnByte", handler -> {
          assertEquals(Byte.valueOf((byte) 8), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnShort", handler -> {
          assertEquals(Short.valueOf((short) 8), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnInteger", handler -> {
          assertEquals(Integer.valueOf(7), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnLong", handler -> {
          assertEquals(Long.valueOf(9), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnFloat", handler -> {
          assertEquals(Float.valueOf(7.9f), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnDouble", handler -> {
          assertEquals(Double.valueOf(8.9), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnChar", handler -> {
          assertEquals(Character.valueOf('c'), handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/p/returnStatusCode", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.NO_CONTENT, ctx.getResponseCode());
        })
        .compile("/p/statusCode", handler -> {
          MockContext ctx = MockContextHelper.mockContext().setPathString("/?statusCode=200&q=*:*");
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.OK, ctx.getResponseCode());
        })
        .compile("/p/noContent", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.NO_CONTENT, ctx.getResponseCode());
        })
        .compile("/p/sideEffect", handler -> {
          MockContext ctx = MockContextHelper.mockContext();
          assertEquals(ctx, handler.apply(ctx));
          assertEquals(StatusCode.CREATED, ctx.getResponseCode());
        })
    ;
  }

  @Test
  public void body() throws Exception {
    new MvcHandlerCompilerRunner(new Provisioning())
        .compile("POST", "/p/bodyMapParam", handler -> {
          Map map = Collections.emptyMap();

          Context ctx = mockContext("POST", "/p/bodyMapParam");
          when(ctx.body(Reified.map(String.class, Object.class).getType())).thenReturn(map);

          assertEquals(map, handler.apply(ctx));
        })
        .compile("POST", "/p/bodyStringParam", handler -> {
          Context ctx = mockContext("POST", "/p/bodyStringParam");
          when(ctx.body(String.class)).thenReturn("...");

          assertEquals("...", handler.apply(ctx));
        })
        .compile("POST", "/p/bodyBytesParam", handler -> {
          assertEquals("...",
              handler.apply(
                  MockContextHelper.mockContext().setBody("...".getBytes(StandardCharsets.UTF_8))));
        })
        .compile("POST", "/p/bodyInputStreamParam", handler -> {
          InputStream stream = mock(InputStream.class);
          Body body = mock(Body.class);
          when(body.stream()).thenReturn(stream);
          assertEquals(stream.toString(),
              handler.apply(MockContextHelper.mockContext().setBody(body)));
        })
        .compile("POST", "/p/bodyChannelParam", handler -> {
          ReadableByteChannel channel = mock(ReadableByteChannel.class);
          Body body = mock(Body.class);
          when(body.channel()).thenReturn(channel);
          assertEquals(channel.toString(),
              handler.apply(MockContextHelper.mockContext().setBody(body)));
        })
        .compile("POST", "/p/bodyBeanParam", handler -> {
          JavaBeanParam bean = mock(JavaBeanParam.class);

          Context ctx = mockContext("POST", "/p/bodyIntParam");
          when(ctx.body(JavaBeanParam.class)).thenReturn(bean);

          assertEquals(bean.toString(), handler.apply(ctx));
        })
        .compile("POST", "/p/bodyIntParam", handler -> {
          Body body = mock(Body.class);
          when(body.intValue()).thenReturn(9);

          Context ctx = mockContext("POST", "/p/bodyIntParam");
          when(ctx.body()).thenReturn(body);

          assertEquals(9, handler.apply(ctx));
        })
        .compile("POST", "/p/bodyOptionalIntParam", handler -> {
          Body body = mock(Body.class);
          when(body.toOptional(Integer.class)).thenReturn(Optional.of(9));

          Context ctx = mockContext("POST", "/p/bodyOptionalIntParam");
          when(ctx.body()).thenReturn(body);

          assertEquals(Optional.of(9), handler.apply(ctx));
        })
        .compile("POST", "/p/bodyCustomGenericParam", handler -> {
          CustomGenericType generic = new CustomGenericType<>();
          Type parameterized = Reified
              .getParameterized(CustomGenericType.class, String.class).getType();
          Context ctx = mockContext("POST", "/p/bodyCustomGenericParam");
          when(ctx.body(parameterized)).thenReturn(generic);
          assertEquals(generic, handler.apply(ctx));
        })
    ;
  }

  @Test
  public void jarxs() throws Exception {
    new MvcHandlerCompilerRunner(new JaxrsController())
        .compile("/jaxrs/query", handler -> {
          assertEquals("v1",
              handler.apply(MockContextHelper.mockContext().setPathString("/?q1=v1")));
        })
        .compile("/jaxrs", handler -> {
          assertEquals("doGet", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("POST", "/jaxrs/post", handler -> {
          assertEquals("doPost", handler.apply(MockContextHelper.mockContext()));
        });
  }

  @Test
  public void noTopLevel() throws Exception {
    new MvcHandlerCompilerRunner(new NoPathRoute())
        .compile("/", handler -> {
          assertEquals("root", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/subpath", handler -> {
          assertEquals("subpath", handler.apply(MockContextHelper.mockContext()));
        })
    ;
  }

  @Test
  public void nullRoutes() throws Exception {
    new MvcHandlerCompilerRunner(new NullRoutes())
        .compile("/nullbean", handler -> {
          assertThrows(ProvisioningException.class,
              () -> handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/nullok", handler -> {
          assertEquals("null", handler.apply(MockContextHelper.mockContext()));
        })
        .compile("/nonnull", handler -> {
          assertThrows(MissingValueException.class,
              () -> handler.apply(MockContextHelper.mockContext()));
        })

    ;
  }

  @Test
  public void routeParam() throws Exception {
    new MvcHandlerCompilerRunner(new RouteInjection())
        .compile("/route", handler -> {
          Route route = mock(Route.class);
          assertEquals(route, handler.apply(MockContextHelper.mockContext().setRoute(route)));
        })
    ;
  }

  @Test
  public void contextPath() throws Exception {
    new MvcHandlerCompilerRunner(new RouteContextPath())
        .compile("/", handler -> {
          Route route = mock(Route.class);
          assertEquals(route, handler.apply(MockContextHelper.mockContext().setRoute(route)));
        })
        .compile("/subpath", handler -> {
          Route route = mock(Route.class);
          assertEquals(route, handler.apply(MockContextHelper.mockContext().setRoute(route)));
        })
    ;
  }

  private Context mockContext(String method, String path) {
    Context ctx = mock(Context.class);
    when(ctx.getMethod()).thenReturn(method);
    when(ctx.pathString()).thenReturn(path);
    return ctx;
  }

  private Map<String, String> mapOf(String... values) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put(values[i], values[i + 1]);
    }
    return map;
  }
}
