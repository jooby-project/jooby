package org.jooby;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

import java.util.function.Supplier;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class })
public class JoobyRunTest {

  @SuppressWarnings("serial")
  public static class ArgEx extends Exception {

    private String[] args;

    public ArgEx(final String[] args) {
      this.args = args;
    }

  }

  public static class NoopApp extends Jooby {
    @Override
    public void start(final String[] args) throws Exception {
    }
  }

  public static class NoopAppEx extends Jooby {
    @Override
    public void start(final String[] args) throws Exception {
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

  @Test
  public void runClassArg() throws Throwable {
    String[] args = {"foo" };
    try {
      Jooby.run(NoopAppEx.class, args);
      fail();
    } catch (ArgEx ex) {
      assertArrayEquals(args, ex.args);
    }
  }
}
