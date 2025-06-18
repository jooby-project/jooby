/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.value.ValueFactory;

public class ValueConverterHelper {

  public static ValueFactory testContext() {
    var result = new ValueFactory();
    return result;
    //    List<BeanConverter> beans = new ArrayList<>();
    //    List<ValueConverter> simple = new ArrayList<>(ValueConverters.defaultConverters());
    //    Stream.of(converters).filter(it -> (!(it instanceof BeanConverter))).forEach(simple::add);
    //    Stream.of(converters)
    //        .filter(it -> (it instanceof BeanConverter))
    //        .forEach(it -> beans.add((BeanConverter) it));
    //
    //    Context ctx = mock(Context.class);
    //    Router router = mock(Router.class);
    //    when(router.getConverters()).thenReturn(simple);
    //    when(router.getBeanConverters()).thenReturn(beans);
    //    when(ctx.getRouter()).thenReturn(router);
    //    when(ctx.convert(any(), any()))
    //        .then(
    //            (Answer)
    //                invocation -> {
    //                  ValueNode value = invocation.getArgument(0);
    //                  Class type = invocation.getArgument(1);
    //                  var result = ValueConverters.convert(value, type, router.getValueFactory());
    //                  if (result == null) {
    //                    throw new TypeMismatchException(value.name(), type);
    //                  }
    //                  return result;
    //                });
    //    when(ctx.convertOrNull(any(), any()))
    //        .then(
    //            (Answer)
    //                invocation -> {
    //                  ValueNode value = invocation.getArgument(0);
    //                  Class type = invocation.getArgument(1);
    //                  return ValueConverters.convert(value, type, router.getValueFactory());
    //                });
    //    return ctx;
  }
}
