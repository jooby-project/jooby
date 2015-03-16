package org.jooby.internal;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.MockUnit;
import org.jooby.internal.reqparam.LocaleParamConverter;
import org.jooby.internal.reqparam.StaticMethodParamConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.TypeLiteral;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StaticMethodTypeConverter.class, LocaleParamConverter.class,
    StaticMethodParamConverter.class })
public class StaticMethodTypeConverterTest {

  @Test
  public void toAnythingElse() throws Exception {
    TypeLiteral<Object> type = TypeLiteral.get(Object.class);
    new MockUnit()
        .expect(unit -> {
          StaticMethodParamConverter converter = unit
              .mockConstructor(StaticMethodParamConverter.class, new Class[]{String.class },
                  "valueOf");
          expect(converter.convert(eq(type), aryEq(new Object[]{"y" }), eq(null))).andReturn(
              "x");
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
          StaticMethodParamConverter converter = unit
              .mockConstructor(StaticMethodParamConverter.class, new Class[]{String.class },
                  "valueOf");
          expect(converter.convert(eq(type), aryEq(new Object[]{"y" }), eq(null)))
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
    assertEquals("forName(java.lang.String)", new StaticMethodTypeConverter<Package>("forName").toString());
  }
}
