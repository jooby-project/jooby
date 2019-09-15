package io.jooby.internal;

import io.jooby.BeanConverter;
import io.jooby.Context;
import io.jooby.Router;
import io.jooby.Value;
import io.jooby.ValueConverter;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueConverterHelper {

  public static Context testContext(ValueConverter... converters) {
    List<BeanConverter> beans = new ArrayList<>();
    List<ValueConverter> simple = ValueConverters.defaultConverters();
    Stream.of(converters).filter(it -> (!(it instanceof BeanConverter)))
        .forEach(simple::add);
    Stream.of(converters).filter(it -> (it instanceof BeanConverter)).forEach(it ->
        beans.add((BeanConverter) it)
    );
    ValueConverters.addFallbackConverters(simple);
    ValueConverters.addFallbackBeanConverters(beans);

    Context ctx = mock(Context.class);
    Router router = mock(Router.class);
    when(router.getConverters()).thenReturn(simple);
    when(router.getBeanConverters()).thenReturn(beans);
    when(ctx.convert(any(), any())).then((Answer) invocation -> {
      Value value = invocation.getArgument(0);
      Class type = invocation.getArgument(1);
      return ValueConverters.convert(value, type, router);
    });
    return ctx;
  }
}
