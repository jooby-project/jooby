package org.jooby;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.function.Supplier;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class, System.class })
public class JoobyRunTest {

  @SuppressWarnings("serial")
  public static class ArgEx extends RuntimeException {

    public ArgEx(final String[] args) {
      super(Arrays.toString(args));
    }

  }

  public static class NoopApp extends Jooby {
    @Override
    public void start(final String[] args) {
    }
  }

  public static class NoopAppEx extends Jooby {
    @Override
    public void start(final String[] args) {
      throw new ArgEx(args);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void runSupplier() throws Exception {
    String[] args = {};
    new MockUnit(Supplier.class, Jooby.class)
        .expect(unit -> {
          Supplier supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start(aryEq(args));
        })
        .run(unit -> {
          Jooby.run(unit.get(Supplier.class), args);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void runSupplierArg() throws Exception {
    String[] args = {"foo" };
    new MockUnit(Supplier.class, Jooby.class)
        .expect(unit -> {
          Supplier supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start(aryEq(args));
        })
        .run(unit -> {
          Jooby.run(unit.get(Supplier.class), args);
        });
  }

  @Test
  public void runClass() throws Throwable {
    Jooby.run(NoopApp.class);
  }

}
