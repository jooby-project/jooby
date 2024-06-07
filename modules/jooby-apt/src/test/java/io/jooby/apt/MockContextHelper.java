/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.jooby.BeanConverter;
import io.jooby.Router;
import io.jooby.ValueConverter;
import io.jooby.test.MockContext;

public class MockContextHelper {

  public static MockContext mockContext(ValueConverter... converters) {
    List<BeanConverter> beans = new ArrayList<>();
    List<ValueConverter> simple = new ArrayList<>(ValueConverter.defaults());
    Stream.of(converters).filter(it -> (!(it instanceof BeanConverter))).forEach(simple::add);
    Stream.of(converters)
        .filter(it -> (it instanceof BeanConverter))
        .forEach(it -> beans.add((BeanConverter) it));

    Router router = mock(Router.class);
    when(router.getConverters()).thenReturn(simple);
    when(router.getBeanConverters()).thenReturn(beans);

    MockContext ctx = new MockContext();
    ctx.setRouter(router);
    return ctx;
  }
}
