package org.jooby;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;

import java.io.File;

import org.jooby.internal.js.JsJooby;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class, File.class })
public class JsJoobyTest {

  @Test
  public void jsboot() throws Exception {
    String jsfile = "app.js";
    String[] args = new String[0];
    new MockUnit(Jooby.class)
        .expect(unit -> {
          JsJooby js = unit.constructor(JsJooby.class)
              .build();

          File file = unit.constructor(File.class)
              .args(String.class)
              .build(jsfile);

          expect(js.run(file)).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start(aryEq(args));
        })
        .run(unit -> {
          Jooby.main(args);
        });

  }

  @Test
  public void jsbootWith1Arg() throws Exception {
    String jsfile = "app.js";
    String[] args = {jsfile, "foo" };
    new MockUnit(Jooby.class)
        .expect(unit -> {
          JsJooby js = unit.constructor(JsJooby.class)
              .build();

          File file = unit.constructor(File.class)
              .args(String.class)
              .build(jsfile);

          expect(js.run(file)).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start(aryEq(new String[]{"foo" }));
        })
        .run(unit -> {
          Jooby.main(args);
        });
  }

  @Test
  public void jsbootWithArgs() throws Exception {
    String jsfile = "app.js";
    String[] args = {jsfile, "foo", "bar" };
    new MockUnit(Jooby.class)
        .expect(unit -> {
          JsJooby js = unit.constructor(JsJooby.class)
              .build();

          File file = unit.constructor(File.class)
              .args(String.class)
              .build(jsfile);

          expect(js.run(file)).andReturn(unit.get(Jooby.class));
        })
        .expect(unit -> {
          Jooby jooby = unit.get(Jooby.class);
          jooby.start(aryEq(new String[]{"foo", "bar" }));
        })
        .run(unit -> {
          Jooby.main(args);
        });
  }

}
