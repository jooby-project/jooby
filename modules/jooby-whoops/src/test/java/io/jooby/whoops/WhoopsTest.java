package io.jooby.whoops;

import io.jooby.MockContext;
import io.jooby.StatusCode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class WhoopsTest {

  @Test
  public void shouldRender() {
    Whoops whoops = new Whoops(basedir());

    whoops.render(new MockContext(), new Throwable(), StatusCode.BAD_REQUEST);
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
