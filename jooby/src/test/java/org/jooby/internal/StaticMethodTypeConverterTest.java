package org.jooby.internal;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.internal.reqparam.LocaleParser;
import org.jooby.internal.reqparam.StaticMethodParser;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.TypeLiteral;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StaticMethodTypeConverter.class, LocaleParser.class,
    StaticMethodParser.class })
public class StaticMethodTypeConverterTest {

  @Test
  public void toAnythingElse() throws Exception {
    TypeLiteral<Object> type = TypeLiteral.get(Object.class);
    new MockUnit()
        .expect(unit -> {
          StaticMethodParser converter = unit
              .mockConstructor(StaticMethodParser.class, new Class[]{String.class },
                  "valueOf");
          expect(converter.parse(eq(type), eq("y"))).andReturn("x");
        })
        .run(unit -> {
          assertEquals("x", new StaticMethodTypeConverter<Object>("valueOf").convert("y", type));
        });
  }

  @Test(expected = IllegalStateException.class)
  public void runtimeError() throws Exception {
    TypeLiteral<Object> type = TypeLiteral.get(Object.class);
    new MockUnit()
        .expect(unit -> {
          StaticMethodParser converter = unit
              .mockConstructor(StaticMethodParser.class, new Class[]{String.class },
                  "valueOf");
          expect(converter.parse(eq(type), eq("y")))
              .andThrow(new IllegalArgumentException("intentional err"));
        })
        .run(unit -> {
          new StaticMethodTypeConverter<Object>("valueOf").convert("y", type);
        });
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked" })
  public void shouldNotMatchEnums() throws Exception {
    TypeLiteral type = TypeLiteral.get(Enum.class);
    new MockUnit()
        .run(unit -> {
          assertEquals(false, new StaticMethodTypeConverter<Object>("valueOf").matches(type));
        });
  }

  @Test
  public void shouldStaticMethod() throws Exception {
    TypeLiteral<Package> type = TypeLiteral.get(Package.class);
    assertEquals(true, new StaticMethodTypeConverter<Package>("getPackage").matches(type));
  }

  @Test
  public void describe() throws Exception {
    assertEquals("forName(String)", new StaticMethodTypeConverter<Package>("forName").toString());
  }
}
