package org.jooby.spec;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import apps.ParseEnums;
import apps.model.Event;

public class EnumsTest extends RouteSpecTest {

  private Path basedir = new File(System.getProperty("user.dir")).toPath();

  @Test
  public void parseEnum() throws Exception {
    routes(new RouteProcessor().process(new ParseEnums(), basedir))
        .next(r -> {
          assertEquals("GET", r.method());
          assertEquals("/", r.pattern());

          params(r.params());

          assertEquals(Event.Frequency.class, r.response().type());
        });
  }
}
