package io.jooby.internal.whoops;

import com.mitchellbosecke.pebble.PebbleEngine;
import io.jooby.MockContext;
import io.jooby.Route;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WhoopsTest {

  @Test
  public void shouldParseTemplates() {
    PebbleEngine engine = Whoops.engine();
    String[] templates = {
        "env_details", "frame_code", "frame_list", "frames_container",
        "frames_description", "header", "header_outer", "layout", "panel_details",
        "panel_details_outer", "panel_left", "panel_left_outer"
    };
    for (String template : templates) {
      engine.getTemplate(template);
    }
  }

  private MockContext newContext() {
    Route route = new Route("GET", "/pattern", ctx -> {
      return ctx;
    });
    MockContext ctx = new MockContext();
    ctx.setRoute(route);
    return ctx;
  }

  private static Path basedir() {
    Path basedir = Paths.get(System.getProperty("user.dir"));
    if (!basedir.getFileName().toString().equals("jooby-whoops")) {
      // IDE vs Maven
      basedir = basedir.resolve("modules").resolve("jooby-whoops");
    }
    return basedir;
  }
}
