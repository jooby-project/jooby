package io.jooby.openapi;

import com.fasterxml.jackson.databind.JavaType;
import examples.Letter;
import examples.RouteFormArgs;
import examples.RouteImport;
import examples.RouteImportReferences;
import examples.RoutePathArgs;
import examples.RouteQueryArgs;
import examples.RouteIdioms;
import examples.RouteInline;
import examples.RoutePatternIdioms;
import examples.RouteReturnTypeApp;
import examples.ABean;
import io.jooby.internal.openapi.DebugOption;
import io.jooby.internal.openapi.HttpType;
import kt.KtCoroutineRouteIdioms;
import kt.KtRouteIdioms;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenApiToolTest {
  @OpenApiTest(value = RoutePatternIdioms.class)
  public void routePatternIdioms(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /variable", route.toString());
        })
        .next(route -> {
          assertEquals("DELETE /variable/{id}", route.toString());
        })
        .next(route -> {
          assertEquals("POST /variable/foo", route.toString());
        })
        .next(route -> {
          assertEquals("PUT /variable/variable/foo", route.toString());
        })
        .verify();
  }

  @OpenApiTest(value = RouteInline.class)
  public void routeInline(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /inline", route.toString());
        })
        .verify();
  }

  @OpenApiTest(value = RouteIdioms.class)
  public void routeIdioms(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /aaa/bbb", route.toString());
        })
        .next(route -> {
          assertEquals("GET /aaa/ccc/ddd", route.toString());
        })
        .next(route -> {
          assertEquals("GET /aaa/eee", route.toString());
        })
        .next(route -> {
          assertEquals("GET /inline", route.toString());
        })
        .next(route -> {
          assertEquals("GET /routeReference", route.toString());
        })
        .next(route -> {
          assertEquals("GET /staticRouteReference", route.toString());
        })
        .next(route -> {
          assertEquals("GET /externalReference", route.toString());
        })
        .next(route -> {
          assertEquals("GET /externalStaticReference", route.toString());
        })
        .next(route -> {
          assertEquals("GET /alonevar", route.toString());
        })
        .next(route -> {
          assertEquals("GET /aloneinline", route.toString());
        })
        .next(route -> {
          assertEquals("GET /lambdaRef", route.toString());
        })
        .verify();
  }

  @OpenApiTest(value = KtRouteIdioms.class)
  public void ktRoute(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /implicitContext", route.toString());
        })
        .next(route -> {
          assertEquals("GET /explicitContext", route.toString());
        })
        .next(route -> {
          assertEquals("GET /api/people", route.toString());
        })
        .next(route -> {
          assertEquals("GET /api/version", route.toString());
        })
        .verify();
  }

  @OpenApiTest(value = KtCoroutineRouteIdioms.class)
  public void ktCoroutineRoute(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /version", route.toString());
        })
        .next(route -> {
          assertEquals("PATCH /api/version", route.toString());
        })
        .next(route -> {
          assertEquals("GET /api/people", route.toString());
        })
        .verify();
  }

  @OpenApiTest(value = RouteQueryArgs.class)
  public void routeQueryArguments(RouteIterator iterator) {
    iterator
        .next((route, args) -> {
          assertEquals("GET /", route.toString());

          args
              .next(it -> {
                assertEquals(String.class.getName(), it.getJavaType());
                assertEquals("str", it.getName());
                assertNull(it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("i", it.getName());
                assertNull(it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.List<java.lang.String>", it.getJavaType());
                assertEquals("listStr", it.getName());
                assertNull(it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.List<java.lang.Double>", it.getJavaType());
                assertEquals("listType", it.getName());
                assertNull(it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(String.class.getName(), it.getJavaType());
                assertEquals("defstr", it.getName());
                assertEquals("default", it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("defint", it.getName());
                assertEquals(87, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("defint0", it.getName());
                assertEquals(0, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(boolean.class.getName(), it.getJavaType());
                assertEquals("defbool", it.getName());
                assertEquals(true, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.String>", it.getJavaType());
                assertEquals("optstr", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.Integer>", it.getJavaType());
                assertEquals("optint", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.String>", it.getJavaType());
                assertEquals("optstr2", it.getName());
                assertEquals("optional", it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.lang.Integer", it.getJavaType());
                assertEquals("toI", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(Letter.class.getName(), it.getJavaType());
                assertEquals("letter", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Map<java.lang.String,java.lang.String>", it.getJavaType());
                assertEquals("query", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Map<java.lang.String,java.util.List<java.lang.String>>",
                    it.getJavaType());
                assertEquals("queryList", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .next(it -> {
                assertEquals(ABean.class.getName(), it.getJavaType());
                assertEquals("query", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.QUERY, it.getHttpType());
              })
              .verify();
        })
        .verify();
  }

  @OpenApiTest(value = RoutePathArgs.class)
  public void routePathArguments(RouteIterator iterator) {
    iterator
        .next((route, args) -> {
          assertEquals("GET /", route.toString());

          args
              .next(it -> {
                assertEquals(String.class.getName(), it.getJavaType());
                assertEquals("str", it.getName());
                assertNull(it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("i", it.getName());
                assertNull(it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.List<java.lang.String>", it.getJavaType());
                assertEquals("listStr", it.getName());
                assertNull(it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.List<java.lang.Double>", it.getJavaType());
                assertEquals("listType", it.getName());
                assertNull(it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(String.class.getName(), it.getJavaType());
                assertEquals("defstr", it.getName());
                assertEquals("default", it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("defint", it.getName());
                assertEquals(87, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("defint0", it.getName());
                assertEquals(0, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(boolean.class.getName(), it.getJavaType());
                assertEquals("defbool", it.getName());
                assertEquals(true, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.String>", it.getJavaType());
                assertEquals("optstr", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.Integer>", it.getJavaType());
                assertEquals("optint", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.String>", it.getJavaType());
                assertEquals("optstr2", it.getName());
                assertEquals("optional", it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.lang.Integer", it.getJavaType());
                assertEquals("toI", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(Letter.class.getName(), it.getJavaType());
                assertEquals("letter", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Map<java.lang.String,java.lang.String>", it.getJavaType());
                assertEquals("path", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Map<java.lang.String,java.util.List<java.lang.String>>",
                    it.getJavaType());
                assertEquals("pathList", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .next(it -> {
                assertEquals(ABean.class.getName(), it.getJavaType());
                assertEquals("path", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.PATH, it.getHttpType());
              })
              .verify();
        })
        .verify();
  }

  @OpenApiTest(value = RouteFormArgs.class)
  public void routeFormArguments(RouteIterator iterator) {
    iterator
        .next((route, args) -> {
          assertEquals("GET /", route.toString());

          args
              .next(it -> {
                assertEquals(String.class.getName(), it.getJavaType());
                assertEquals("str", it.getName());
                assertNull(it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("i", it.getName());
                assertNull(it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.List<java.lang.String>", it.getJavaType());
                assertEquals("listStr", it.getName());
                assertNull(it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.List<java.lang.Double>", it.getJavaType());
                assertEquals("listType", it.getName());
                assertNull(it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(String.class.getName(), it.getJavaType());
                assertEquals("defstr", it.getName());
                assertEquals("default", it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("defint", it.getName());
                assertEquals(87, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(int.class.getName(), it.getJavaType());
                assertEquals("defint0", it.getName());
                assertEquals(0, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(boolean.class.getName(), it.getJavaType());
                assertEquals("defbool", it.getName());
                assertEquals(true, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.String>", it.getJavaType());
                assertEquals("optstr", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.Integer>", it.getJavaType());
                assertEquals("optint", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Optional<java.lang.String>", it.getJavaType());
                assertEquals("optstr2", it.getName());
                assertEquals("optional", it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.lang.Integer", it.getJavaType());
                assertEquals("toI", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(Letter.class.getName(), it.getJavaType());
                assertEquals("letter", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertTrue(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Map<java.lang.String,java.lang.String>", it.getJavaType());
                assertEquals("form", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals("java.util.Map<java.lang.String,java.util.List<java.lang.String>>",
                    it.getJavaType());
                assertEquals("formList", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertTrue(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .next(it -> {
                assertEquals(ABean.class.getName(), it.getJavaType());
                assertEquals("form", it.getName());
                assertEquals(null, it.getDefaultValue());
                assertFalse(it.isRequired());
                assertFalse(it.isSingle());
                assertEquals(HttpType.FORM, it.getHttpType());
              })
              .verify();
        })
        .verify();
  }

  public static class Java {
    private JavaType type;

    public JavaType getType() {
      return type;
    }

    public void setType(JavaType type) {
      this.type = type;
    }

    @Override public String toString() {
      return type.toString();
    }
  }

  @OpenApiTest(value = RouteReturnTypeApp.class, ignoreArguments = true)
  public void routeReturnType(RouteIterator iterator) {
    //    ObjectMapper mapper = new ObjectMapper();
    //    Java java = new Java();
    //    java.type = mapper.constructType(int[].class);
    //    String json = mapper.writeValueAsString(java);
    //    System.out.println(json);
    //    Java j = mapper.readValue(json, Java.class);
    //    System.out.println(j);
    iterator
        .next(route -> {
          assertEquals("GET /literal/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /literal/2", route.toString());
          assertEquals(Integer.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /literal/3", route.toString());
          assertEquals(Object.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /literal/4", route.toString());
          assertEquals(Boolean.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /call/1", route.toString());
          assertEquals(RouteReturnTypeApp.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /call/2", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /call/3", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /call/4", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /call/5", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /call/6", route.toString());
          assertEquals("java.util.List<java.lang.String>", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /generic/1", route.toString());
          assertEquals(CompletableFuture.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /generic/2", route.toString());
          assertEquals(CompletableFuture.class.getName() + "<" + Integer.class.getName() + ">",
              route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /generic/3", route.toString());
          assertEquals(CompletableFuture.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /generic/4", route.toString());
          assertEquals(Callable.class.getName() + "<" + Byte.class.getName() + ">",
              route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /generic/5", route.toString());
          assertEquals(Callable.class.getName() + "<" + Character.class.getName() + ">",
              route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /localvar/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /localvar/2", route.toString());
          assertEquals(Integer.class.getName(), route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /localvar/3", route.toString());
          assertEquals("[Ljava.lang.String;", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /localvar/4", route.toString());
          assertEquals("[F", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /complexType/1", route.toString());
          assertEquals("java.util.List", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /complexType/2", route.toString());
          assertEquals("java.util.List<java.lang.String>", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /complexType/3", route.toString());
          assertEquals("java.util.List<java.util.List<java.lang.String>>",
              route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /multipleTypes/1", route.toString());
          assertEquals("java.util.ArrayList", route.getReturnTypes().get(0).getJavaType());
          assertEquals("java.util.LinkedList", route.getReturnTypes().get(1).getJavaType());
        })
        .next(route -> {
          assertEquals("GET /multipleTypes/2", route.toString());
          assertEquals("examples.ABean", route.getReturnTypes().get(0).getJavaType());
          assertEquals("examples.BBean", route.getReturnTypes().get(1).getJavaType());
        })
        .next(route -> {
          assertEquals("GET /multipleTypes/3", route.toString());
          assertEquals("examples.Bean", route.getReturnTypes().get(0).getJavaType());
        })
        .next(route -> {
          assertEquals("GET /array/1", route.toString());
          assertEquals("[Z", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /array/2", route.toString());
          assertEquals("[Lexamples.RouteReturnTypeApp;", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /array/3", route.toString());
          assertEquals("[I", route.getReturnType().getJavaType());
        })
        .next(route -> {
          assertEquals("GET /array/4", route.toString());
          assertEquals("[Ljava.lang.String;", route.getReturnType().getJavaType());
        })
        .verify();
  }

  @OpenApiTest(value = RouteImport.class)
  public void routeImport(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /main/a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /main/submain/a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /require/a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /subroute/a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .verify();
  }

  @OpenApiTest(value = RouteImportReferences.class)
  public void routeImportReferences(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("GET /a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /require/a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .next(route -> {
          assertEquals("GET /prefix/a/1", route.toString());
          assertEquals(String.class.getName(), route.getReturnType().toString());
        })
        .verify();
  }
}
