package tests;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.MockRouter;
import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.exception.MissingValueException;
import io.jooby.MockContext;
import io.jooby.Multipart;
import io.jooby.exception.ProvisioningException;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.StatusCode;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HandlerCompilerTest {

  @Test
  public void typeInjection() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals("noarg", router.get("/p/noarg").value());

          assertEquals("ctx", router.get("/p/context").value());

          assertEquals("queryString", router.get("/p/queryString").value());

          assertEquals("formdata", router.get("/p/formdata").value());

          assertEquals("multipart", router.get("/p/multipart").value());

          assertEquals("ctxfirst", router.get("/p/contextFirst").value());

          assertEquals("flashMap", router.get("/p/flashMap").value());

          assertEquals("session:false", router.get("/p/sessionOrNull").value());

          assertEquals("session", router.get("/p/session").value());
        });
  }

  @Test
  public void queryParam() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("search",
              router.get("/p/queryParam",
                  MockContextHelper.mockContext().setQueryString("?q=search"))
                  .value());
        })
    ;
  }

  @Test
  public void cookieParam() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("cookie",
              router.get("/p/cookieParam",
                  MockContextHelper.mockContext().setCookieMap(mapOf("c", "cookie"))).value());
        })
    ;
  }

  @Test
  public void headerParam() throws Exception {
    long epoc = System.currentTimeMillis();
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals(String.valueOf(epoc),
              router.get("/p/headerParam",
                  MockContextHelper.mockContext().setRequestHeader("instant", String.valueOf(epoc)))
                  .value());
        })
    ;
  }

  @Test
  public void flashParam() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("hey",
              router.get("/p/flashParam",
                  MockContextHelper.mockContext().setFlashAttribute("message", "hey")).value());
        })
    ;
  }

  @Test
  public void formParam() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          MockContext ctx = MockContextHelper.mockContext();
          Multipart formdata = Multipart.create(ctx);
          formdata.put("name", "yo");

          assertEquals("yo",
              router.get("/p/formParam", ctx.setMultipart(formdata)).value());
        })
    ;
  }

  @Test
  public void pathParam() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);
          assertEquals("[bar]",
              router.get("/p/listBeanPathParam/bar",
                  MockContextHelper.mockContext()).value());

          assertEquals("v1",
              router.get("/p/pathParam/v1",
                  MockContextHelper.mockContext()).value());

          assertEquals("2",
              router.get("/p/bytePathParam/2",
                  MockContextHelper.mockContext()).value());

          assertEquals("3",
              router.get("/p/longPathParam/3",
                  MockContextHelper.mockContext()).value());

          assertEquals("3.5",
              router.get("/p/floatPathParam/3.5",
                  MockContextHelper.mockContext()).value());

          assertEquals("3.55",
              router.get("/p/doublePathParam/3.55",
                  MockContextHelper.mockContext()).value());

          assertEquals("true",
              router.get("/p/booleanPathParam/true",
                  MockContextHelper.mockContext()).value());

          assertEquals("1",
              router.get("/p/intPathParam/1",
                  MockContextHelper.mockContext()).value());

          assertEquals("Optional[x]",
              router.get("/p/optionalStringPathParam/x",
                  MockContextHelper.mockContext()).value());

          assertEquals("Optional[7]",
              router.get("/p/optionalIntPathParam/7",
                  MockContextHelper.mockContext()).value());

          assertEquals("bar",
              router.get("/p/javaBeanPathParam/bar",
                  MockContextHelper.mockContext()).value());

          assertEquals("[bar]",
              router.get("/p/listStringPathParam/bar",
                  MockContextHelper.mockContext()).value());

          assertEquals("[6.7]",
              router.get("/p/listDoublePathParam/6.7",
                  MockContextHelper.mockContext()).value());

          assertEquals("[bar]",
              router.get("/p/setStringPathParam/bar",
                  MockContextHelper.mockContext()).value());

          assertEquals("[6.7]",
              router.get("/p/setDoublePathParam/6.7",
                  MockContextHelper.mockContext()).value());

          assertEquals("[bar]",
              router.get("/p/setBeanPathParam/bar",
                  MockContextHelper.mockContext()).value());

          assertEquals("A",
              router.get("/p/enumParam/a",
                  MockContextHelper.mockContext()).value());

          assertEquals("Optional[A]",
              router.get("/p/optionalEnumParam/a",
                  MockContextHelper.mockContext()).value());

          assertEquals("[B]",
              router.get("/p/listEnumParam/b",
                  MockContextHelper.mockContext()).value());

          assertEquals("9",
              router.get("/p/primitiveWrapper/9",
                  MockContextHelper.mockContext()).value());
        })
    ;
  }

  @Test
  public void multipleParameters() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals("123GET /p/parameters/1231x",
              router.get("/p/parameters/123",
                  MockContextHelper.mockContext().setQueryString("?offset=1&foo=x")).value());
        });
  }

  @Test
  public void fileParam() throws Exception {
    FileUpload file = mock(FileUpload.class);
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals(file.toString(),
              router.get("/p/fileParam",
                  MockContextHelper.mockContext().setFile("file", file)).value());

          assertEquals(Arrays.asList(file).toString(),
              router.get("/p/fileParams",
                  MockContextHelper.mockContext().setFile("file", file)).value());
        });
  }

  @Test
  public void specialParam() throws Exception {
    UUID uuid = UUID.randomUUID();
    BigDecimal bigDecimal = new BigDecimal("88.6");
    BigInteger bigInteger = new BigInteger("888888");
    Charset charset = StandardCharsets.UTF_8;

    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals(uuid.toString(),
              router.get("/p/uuidParam",
                  MockContextHelper.mockContext().setQueryString("?value=" + uuid.toString()))
                  .value());

          assertEquals(bigDecimal.toString(),
              router.get("/p/bigDecimalParam",
                  MockContextHelper.mockContext().setQueryString("?value=" + bigDecimal.toString()))
                  .value());

          assertEquals(bigInteger.toString(),
              router.get("/p/bigIntegerParam",
                  MockContextHelper.mockContext().setQueryString("?value=" + bigInteger.toString()))
                  .value());

          assertEquals(charset.toString(),
              router.get("/p/charsetParam",
                  MockContextHelper.mockContext().setQueryString("?value=" + charset.toString()))
                  .value());

          Path path = mock(Path.class);
          FileUpload file = mock(FileUpload.class);
          when(file.path()).thenReturn(path);
          assertEquals(path.toString(),
              router.get("/p/pathFormParam",
                  MockContextHelper.mockContext().setFile("file", file)).value());
        });
  }

  @Test
  public void returnTypes() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals(Byte.valueOf((byte) 8),
              router.get("/p/returnByte", MockContextHelper.mockContext()).value());

          assertEquals(Short.valueOf((short) 8),
              router.get("/p/returnShort", MockContextHelper.mockContext()).value());

          assertEquals(Integer.valueOf(7),
              router.get("/p/returnInteger", MockContextHelper.mockContext()).value());

          assertEquals(Long.valueOf(9),
              router.get("/p/returnLong", MockContextHelper.mockContext()).value());

          assertEquals(Float.valueOf(7.9f),
              router.get("/p/returnFloat", MockContextHelper.mockContext()).value());

          assertEquals(Double.valueOf(8.9),
              router.get("/p/returnDouble", MockContextHelper.mockContext()).value());

          assertEquals(Character.valueOf('c'),
              router.get("/p/returnChar", MockContextHelper.mockContext()).value());

          MockContext ctx = MockContextHelper.mockContext();
          assertEquals(ctx,
              router.get("/p/returnStatusCode", ctx).value());
          assertEquals(StatusCode.NO_CONTENT, ctx.getResponseCode());

          assertEquals(ctx,
              router.get("/p/statusCode", ctx.setQueryString("?statusCode=200&q=*:*")).value());
          assertEquals(StatusCode.OK, ctx.getResponseCode());

          ctx = MockContextHelper.mockContext();
          assertEquals(ctx,
              router.delete("/p/noContent", ctx.setQueryString(null)).value());
          assertEquals(StatusCode.NO_CONTENT, ctx.getResponseCode());

          ctx = MockContextHelper.mockContext();
          assertEquals(ctx, router.get("/p/sideEffect", ctx).value());
          assertEquals(StatusCode.CREATED, ctx.getResponseCode());
        });
  }

  @Test
  public void body() throws Exception {
    new MvcModuleCompilerRunner(new Provisioning())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          Map map = Collections.emptyMap();

          Context ctx = mockContext("POST", "/p/bodyMapParam");
          when(ctx.body(Reified.map(String.class, Object.class).getType())).thenReturn(map);

          assertEquals(map, router.post("/p/bodyMapParam", ctx).value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          Context ctx = mockContext("POST", "/p/bodyStringParam");
          when(ctx.body(String.class)).thenReturn("...");

          assertEquals("...", router.post("/p/bodyStringParam", ctx).value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals("...", router.post("/p/bodyBytesParam",
              MockContextHelper.mockContext().setBody("...".getBytes(StandardCharsets.UTF_8)))
              .value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          InputStream stream = mock(InputStream.class);
          Body body = mock(Body.class);
          when(body.stream()).thenReturn(stream);

          assertEquals(stream.toString(), router.post("/p/bodyInputStreamParam",
              MockContextHelper.mockContext().setBody(body))
              .value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);
          ReadableByteChannel channel = mock(ReadableByteChannel.class);
          Body body = mock(Body.class);
          when(body.channel()).thenReturn(channel);

          assertEquals(channel.toString(), router.post("/p/bodyChannelParam",
              MockContextHelper.mockContext().setBody(body))
              .value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          JavaBeanParam bean = mock(JavaBeanParam.class);

          Context ctx = mockContext("POST", "/p/bodyBeanParam");
          when(ctx.body(JavaBeanParam.class)).thenReturn(bean);

          assertEquals(bean.toString(), router.post("/p/bodyBeanParam", ctx).value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          Body body = mock(Body.class);
          when(body.intValue()).thenReturn(9);

          Context ctx = mockContext("POST", "/p/bodyIntParam");
          when(ctx.body()).thenReturn(body);

          assertEquals(9, router.post("/p/bodyIntParam", ctx).value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          Body body = mock(Body.class);
          when(body.toOptional(Integer.class)).thenReturn(Optional.of(9));

          Context ctx = mockContext("POST", "/p/bodyOptionalIntParam");
          when(ctx.body()).thenReturn(body);

          assertEquals(Optional.of(9), router.post("/p/bodyOptionalIntParam", ctx).value());
        })
        .module(app -> {
          MockRouter router = new MockRouter(app);

          CustomGenericType generic = new CustomGenericType<>();
          Type parameterized = Reified
              .getParameterized(CustomGenericType.class, String.class).getType();
          Context ctx = mockContext("POST", "/p/bodyCustomGenericParam");
          when(ctx.body(parameterized)).thenReturn(generic);

          assertEquals(generic, router.post("/p/bodyCustomGenericParam", ctx).value());
        })
    ;
  }

  @Test
  public void jarxs() throws Exception {
    new MvcModuleCompilerRunner(new JaxrsController())
        .module(app -> {
          MockRouter router = new MockRouter(app);

          assertEquals("v1",
              router.get("/jaxrs/query", MockContextHelper.mockContext().setQueryString("?q1=v1"))
                  .value());

          assertEquals("doGet",
              router.get("/jaxrs", MockContextHelper.mockContext()).value());

          assertEquals("doPost",
              router.post("/jaxrs/post", MockContextHelper.mockContext()).value());
        })
    ;
  }

  @Test
  public void noTopLevel() throws Exception {
    new MvcModuleCompilerRunner(new NoPathRoute()).module(app -> {
      MockRouter router = new MockRouter(app);

      assertEquals("root",
          router.get("/", MockContextHelper.mockContext()).value());

      assertEquals("subpath",
          router.get("/subpath", MockContextHelper.mockContext()).value());
    });
  }

  @Test
  public void nullRoutes() throws Exception {
    new MvcModuleCompilerRunner(new NullRoutes()).module(app -> {
      MockRouter router = new MockRouter(app);

      assertThrows(ProvisioningException.class,
          () -> router.get("/nullbean", MockContextHelper.mockContext()).value());

      assertEquals("null",
          router.get("/nullok", MockContextHelper.mockContext()).value());

      assertThrows(MissingValueException.class, () ->
          router.get("/nonnull", MockContextHelper.mockContext()).value());
    });
  }

  @Test
  public void routeParam() throws Exception {
    new MvcModuleCompilerRunner(new RouteInjection()).module(app -> {
      MockRouter router = new MockRouter(app);

      assertTrue(router.get("/route", MockContextHelper.mockContext()).value() instanceof Route);
    });
  }

  @Test
  public void contextPath() throws Exception {
    new MvcModuleCompilerRunner(new RouteContextPath()).module(app -> {
      MockRouter router = new MockRouter(app);

      assertEquals("/", router.get("/", MockContextHelper.mockContext()).value());

      assertEquals("/subpath", router.get("/subpath", MockContextHelper.mockContext()).value());
    });
  }

  private Context mockContext(String method, String path) {
    Context ctx = mock(Context.class);
    when(ctx.getMethod()).thenReturn(method);
    when(ctx.getRequestPath()).thenReturn(path);
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
