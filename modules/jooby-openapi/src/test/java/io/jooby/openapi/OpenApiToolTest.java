package io.jooby.openapi;

import examples.RouteIdioms;
import examples.RouteInline;
import examples.RoutePatternIdioms;
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

  @OpenApiTest(value = KtRouteIdioms.class, debug = true)
  public void ktRouteInline(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("/inline", route.getPattern());
        })
        .verify();
  }
}
