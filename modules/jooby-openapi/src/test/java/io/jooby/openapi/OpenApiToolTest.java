package io.jooby.openapi;

import examples.RouteIdioms;
import examples.RouteInline;
import examples.RoutePatternIdioms;
import io.jooby.internal.openapi.DebugOption;
import kt.KtCoroutineRouteIdioms;
import kt.KtRouteIdioms;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenApiToolTest {
  @OpenApiTest(value = RoutePatternIdioms.class)
  public void routePatternIdioms(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("/variable", route.getPattern());
        })
        .next(route -> {
          assertEquals("/variable/{id}", route.getPattern());
        })
        .next(route -> {
          assertEquals("/variable/foo", route.getPattern());
        })
        .next(route -> {
          assertEquals("/variable/variable/foo", route.getPattern());
        })
        .verify();
  }

  @OpenApiTest(value = RouteInline.class)
  public void routeInline(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("/inline", route.getPattern());
        })
        .verify();
  }

  @OpenApiTest(value = RouteIdioms.class)
  public void routeIdioms(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("/aaa/bbb", route.getPattern());
        })
        .next(route -> {
          assertEquals("/aaa/ccc/ddd", route.getPattern());
        })
        .next(route -> {
          assertEquals("/aaa/eee", route.getPattern());
        })
        .next(route -> {
          assertEquals("/inline", route.getPattern());
        })
        .next(route -> {
          assertEquals("/routeReference", route.getPattern());
        })
        .next(route -> {
          assertEquals("/staticRouteReference", route.getPattern());
        })
        .next(route -> {
          assertEquals("/externalReference", route.getPattern());
        })
        .next(route -> {
          assertEquals("/externalStaticReference", route.getPattern());
        })
        .next(route -> {
          assertEquals("/alonevar", route.getPattern());
        })
        .next(route -> {
          assertEquals("/aloneinline", route.getPattern());
        })
        .next(route -> {
          assertEquals("/lambdaRef", route.getPattern());
        })
        .verify();
  }

  @OpenApiTest(value = KtRouteIdioms.class)
  public void ktRoute(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("/implicitContext", route.getPattern());
        })
        .next(route -> {
          assertEquals("/explicitContext", route.getPattern());
        })
        .next(route -> {
          assertEquals("/api/people", route.getPattern());
        })
        .next(route -> {
          assertEquals("/api/version", route.getPattern());
        })
        .verify();
  }

  @OpenApiTest(value = KtCoroutineRouteIdioms.class)
  public void ktCoroutineRoute(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("/version", route.getPattern());
        })
        .next(route -> {
          assertEquals("/api/version", route.getPattern());
        })
        .next(route -> {
          assertEquals("/api/people", route.getPattern());
        })
        .verify();
  }
}
