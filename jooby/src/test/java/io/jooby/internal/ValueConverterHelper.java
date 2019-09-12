package io.jooby.internal;

import io.jooby.Context;
import io.jooby.DefaultContext;
import io.jooby.ForwardingContext;
import io.jooby.Jooby;
import io.jooby.Router;
import io.jooby.Value;
import io.jooby.spi.ValueConverter;
import org.mockito.stubbing.Answer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueConverterHelper {

  public static Context testContext(ValueConverter... converters) {
    List<ValueConverter> all = ValueConverters.defaultConverters();
    Stream.of(converters).forEach(all::add);
    ValueConverters.addFallbackConverters(all);

    Context ctx = mock(Context.class);

    when(ctx.convert(any(), any())).then((Answer) invocation -> {
      Value value = invocation.getArgument(0);
      Class type = invocation.getArgument(1);
      return ValueConverters.convert(value, type, all);
    });
    return ctx;
  }
}
