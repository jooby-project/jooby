package org.jooby.apitool;

import com.google.common.collect.Iterators;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

class RouteMethodAssert {

  public static class RouteParameterMatcher {
    private final Iterator<RouteParameter> parameters;
    private RouteParameter param;

    public RouteParameterMatcher(Iterator<RouteParameter> parameters,
        Consumer<RouteParameterMatcher> consumer) {
      this.parameters = parameters;
      param(consumer);
    }

    public RouteParameterMatcher param(Consumer<RouteParameterMatcher> consumer) {
      this.param = parameters.next();
      consumer.accept(this);
      return this;
    }

    public RouteParameterMatcher name(final String name) {
      assertEquals(name, param.name());
      return this;
    }

    public RouteParameterMatcher type(final Type type) {
      assertEquals(type.getTypeName(), param.type().getTypeName());
      return this;
    }

    public RouteParameterMatcher type(final String type) {
      assertEquals(type, param.type().getTypeName());
      return this;
    }

    public RouteParameterMatcher type(final Class type) {
      assertEquals(type.getTypeName(), param.type().getTypeName());
      return this;
    }

    public RouteParameterMatcher value(final Object value) {
      assertEquals(value, param.value());
      return this;
    }

    public RouteParameterMatcher kind(final RouteParameter.Kind kind) {
      assertEquals(kind, param.kind());
      return this;
    }

    public boolean isOptional() {
      return param.optional();
    }

    public RouteParameterMatcher description(final String description) {
      assertEquals(description, param.description().orElse(null));
      return this;
    }
  }

  private RouteMethod route;

  private Iterator<RouteMethod> routes;

  private Iterator<RouteParameter> params;

  public RouteMethodAssert(final List<RouteMethod> methods) {
    this.routes = methods.iterator();
  }

  public RouteMethodAssert next(final Consumer<RouteMethodAssert> consumer) {
    if (params != null && params.hasNext()) {
      fail(route.toString());
    }
    route = routes.next();
    params = route.parameters().iterator();
    consumer.accept(this);
    return this;
  }

  public RouteParameterMatcher param(final Consumer<RouteParameterMatcher> consumer) {
    return new RouteParameterMatcher(params, consumer);
  }

  public void returnType(Type type) {
    assertEquals(type, route.response().type());
  }

  public void returnType(String type) {
    assertEquals(type, route.response().type().getTypeName());
  }

  public void method(final String method) {
    assertEquals(method, route.method());
  }

  public void pattern(final String pattern) {
    assertEquals(pattern, route.pattern());
  }

  public void returnType(Class<?> type) {
    assertEquals(type.getTypeName(), route.response().type().getTypeName());
  }

  public void summary(final String summary) {
    assertEquals(summary, route.summary().orElse(null));
  }

  public void description(final String description) {
    assertEquals(description, route.description().orElse(null));
  }

  public void returns(final String returns) {
    assertEquals(returns, route.response().description().orElse(null));
  }

  public void status(final int status, final String success) {
    assertEquals(success, route.response().status(status).orElse(null));
  }

  public void consumes(final String... consumes) {
    assertEquals(Arrays.asList(consumes), route.consumes());
  }

  public void produces(final String... produces) {
    assertEquals(Arrays.asList(produces), route.produces());
  }

  public void attributes(final Map<String, Object> attributes) {
    assertEquals(attributes, route.attributes());
  }

  public void hasNoParameters() {
    assertEquals(0, route.parameters().size());
  }

  public void done() {
    if (params != null && params.hasNext()) {
      fail(route.toString());
    }
    if (routes.hasNext()) {
      fail(Iterators.toString(routes));
    }
  }
}
