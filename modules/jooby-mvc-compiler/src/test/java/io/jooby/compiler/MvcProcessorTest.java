package io.jooby.compiler;

import io.jooby.Context;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.MockContext;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Session;
import org.junit.jupiter.api.Test;
import source.EnumParam;
import source.JavaBeanParam;
import source.Provisioning;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class MvcProcessorTest {

  @Test
  public void typeInjection() throws Exception {
    new TestProcessor(new Provisioning())
        .compile("noarg", args(), handler -> {
          assertEquals("noarg", handler.apply(new MockContext()));
        })
        .compile("context", args(Context.class), handler -> {
          assertEquals("ctx", handler.apply(new MockContext()));
        })
        .compile("queryString", args(QueryString.class), handler -> {
          assertEquals("queryString", handler.apply(new MockContext()));
        })
        .compile("queryStringOptional", args(Optional.class), handler -> {
          assertEquals("queryStringOptional:true", handler.apply(new MockContext()));
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
        .compile("sessionOrNull", args(Optional.class), handler -> {
          assertEquals("session:false", handler.apply(new MockContext()));
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
        .compile("optionalStringPathParam", args(Optional.class), handler -> {
          assertEquals("Optional[x]",
              handler.apply(new MockContext().setPathMap(mapOf("p1", "x"))));
        })
        .compile("optionalIntPathParam", args(Optional.class), handler -> {
          assertEquals("Optional[7]",
              handler.apply(new MockContext().setPathMap(mapOf("p1", "7"))));
        })
        .compile("javaBeanPathParam", args(JavaBeanParam.class), handler -> {
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
