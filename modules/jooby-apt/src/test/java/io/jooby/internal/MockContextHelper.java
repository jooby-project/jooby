package io.jooby.internal;

import io.jooby.MockContext;
import io.jooby.Router;
import io.jooby.ValueConverter;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockContextHelper {

  public static MockContext mockContext(ValueConverter... converters) {
    List<ValueConverter> all = ValueConverters.defaultConverters();
    Stream.of(converters).forEach(all::add);
    ValueConverters.addFallbackConverters(all);

    Router router = mock(Router.class);
    when(router.getConverters()).thenReturn(all);

    MockContext ctx = new MockContext();
    ctx.setRouter(router);
    return ctx;
  }
}
