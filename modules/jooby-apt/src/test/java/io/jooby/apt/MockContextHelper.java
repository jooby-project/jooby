/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static org.mockito.Mockito.mock;

import java.util.Map;

import io.jooby.Router;
import io.jooby.test.MockContext;
import io.jooby.value.Converter;
import io.jooby.value.ValueFactory;

public class MockContextHelper {

  public static MockContext mockContext() {
    return mockContext(Map.of());
  }

  public static MockContext mockContext(Map<Class<?>, Converter> converters) {
    //    List<BeanConverter> beans = new ArrayList<>();
    //    List<ValueConverter> simple = new ArrayList<>(ValueConverter.defaults());
    //    Stream.of(converters).filter(it -> (!(it instanceof BeanConverter))).forEach(simple::add);
    //    Stream.of(converters)
    //        .filter(it -> (it instanceof BeanConverter))
    //        .forEach(it -> beans.add((BeanConverter) it));

    Router router = mock(Router.class);
    var factory = new ValueFactory();
    converters.forEach(factory::put);
    MockContext ctx = new MockContext();
    ctx.setValueFactory(factory);
    ctx.setRouter(router);
    return ctx;
  }
}
