package io.jooby.compiler;

import io.jooby.Context;
import io.jooby.FlashMap;
import io.jooby.Formdata;
import io.jooby.MockContext;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Session;
import org.junit.jupiter.api.Test;
import source.JavaBeanParam;
import source.Provisioning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
          assertEquals("[bar]", handler.apply(new MockContext().setPathMap(mapOf("values", "bar"))));
        })
        .compile("listDoublePathParam", args(List.class), handler -> {
          assertEquals("[6.7]", handler.apply(new MockContext().setPathMap(mapOf("values", "6.7"))));
        })
        .compile("listBeanPathParam", args(List.class), handler -> {
          assertEquals("[bar]", handler.apply(new MockContext().setPathMap(mapOf("foo", "bar"))));
        });
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
