package org.jooby;

import org.jooby.test.MockUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.function.Supplier;

import static org.easymock.EasyMock.expect;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JoobyJs.class, File.class})
public class JsBootTest {

  @BeforeClass
  public static void before() {
    System.setProperty("logback.configurationFile", "logback.xml");
  }

  @AfterClass
  public static void after() {
    System.setProperty("logback.configurationFile", "");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jsboot() throws Exception {
    String jsfile = "app.js";
    String[] args = new String[0];
    new MockUnit(Jooby.class, Supplier.class)
        .expect(unit -> {
          Supplier<Jooby> supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          JoobyJs js = unit.constructor(JoobyJs.class)
              .build();

          File file = unit.constructor(File.class)
              .args(String.class)
              .build(jsfile);

          expect(js.run(file)).andReturn(unit.get(Supplier.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start(args);
        })
        .run(unit -> {
          JoobyJs.main(args);
        });

  }

  @SuppressWarnings("unchecked")
  @Test
  public void jsbootWith1Arg() throws Exception {
    String jsfile = "app.js";
    String[] args = {jsfile, "foo"};
    new MockUnit(Jooby.class, Supplier.class)
        .expect(unit -> {
          Supplier<Jooby> supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          JoobyJs js = unit.constructor(JoobyJs.class)
              .build();

          File file = unit.constructor(File.class)
              .args(String.class)
              .build(jsfile);

          expect(js.run(file)).andReturn(unit.get(Supplier.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start("foo");
        })
        .run(unit -> {
          JoobyJs.main(args);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void jsbootWithArgs() throws Exception {
    String jsfile = "app.js";
    String[] args = {jsfile, "foo", "bar"};
    new MockUnit(Jooby.class, Supplier.class)
        .expect(unit -> {
          Supplier<Jooby> supplier = unit.get(Supplier.class);
          expect(supplier.get()).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          JoobyJs js = unit.constructor(JoobyJs.class)
              .build();

          File file = unit.constructor(File.class)
              .args(String.class)
              .build(jsfile);

          expect(js.run(file)).andReturn(unit.get(Supplier.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start("foo", "bar");
        })
        .run(unit -> {
          JoobyJs.main(args);
        });
  }

}
