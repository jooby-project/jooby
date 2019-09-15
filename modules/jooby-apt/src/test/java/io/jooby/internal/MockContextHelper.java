package io.jooby.internal;

import io.jooby.BeanConverter;
import io.jooby.MockContext;
import io.jooby.Router;
import io.jooby.ValueConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockContextHelper {

  public static MockContext mockContext(ValueConverter... converters) {
    List<BeanConverter> beans = new ArrayList<>();
    List<ValueConverter> simple = ValueConverters.defaultConverters();
    Stream.of(converters).filter(it -> (!(it instanceof BeanConverter)))
        .forEach(simple::add);
    Stream.of(converters).filter(it -> (it instanceof BeanConverter)).forEach(it ->
        beans.add((BeanConverter) it)
    );
    ValueConverters.addFallbackConverters(simple);
    ValueConverters.addFallbackBeanConverters(beans);

    Router router = mock(Router.class);
    when(router.getConverters()).thenReturn(simple);
    when(router.getBeanConverters()).thenReturn(beans);

    MockContext ctx = new MockContext();
    ctx.setRouter(router);
    return ctx;
  }
}
