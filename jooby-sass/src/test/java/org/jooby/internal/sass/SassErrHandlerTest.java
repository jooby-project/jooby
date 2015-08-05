package org.jooby.internal.sass;

import static org.easymock.EasyMock.expect;

import org.jooby.Err;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.slf4j.Logger;
import org.w3c.css.sac.CSSParseException;

public class SassErrHandlerTest {

  private Block csserr = unit -> {
    CSSParseException err = unit.get(CSSParseException.class);

    expect(err.getURI()).andReturn("test.scss");
    expect(err.getLineNumber()).andReturn(1);
    expect(err.getColumnNumber()).andReturn(3);
    expect(err.getMessage()).andReturn("Err");
  };

  @Test
  public void shouldLogWarn() throws Exception {
    new MockUnit(Logger.class, CSSParseException.class)
        .expect(csserr)
        .expect(unit -> {
          unit.get(Logger.class).warn("{}:{}:{}\n\t{}", "test.scss", 1, 3, "Err");
        })
        .run(unit -> {
          new SassErrHandler(unit.get(Logger.class))
              .warning(unit.get(CSSParseException.class));
        });
  }

  @Test
  public void validate() throws Exception {
    new MockUnit(Logger.class)
        .run(unit -> {
          SassErrHandler handler = new SassErrHandler(unit.get(Logger.class));
          handler.validate();
        });
  }

  @Test(expected = Err.class)
  public void validateErr() throws Exception {
    new MockUnit(Logger.class, CSSParseException.class)
        .expect(csserr)
        .run(unit -> {
          SassErrHandler handler = new SassErrHandler(unit.get(Logger.class));
          handler.error(unit.get(CSSParseException.class));
          handler.validate();
        });
  }

  @Test(expected = Err.class)
  public void validateFatalErr() throws Exception {
    new MockUnit(Logger.class, CSSParseException.class)
        .expect(csserr)
        .run(unit -> {
          SassErrHandler handler = new SassErrHandler(unit.get(Logger.class));
          handler.fatalError(unit.get(CSSParseException.class));
          handler.validate();
        });
  }

}
