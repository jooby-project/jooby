package io.jooby.pebble;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.typesafe.config.ConfigFactory;
import io.jooby.Environment;
import io.jooby.MockContext;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PebbleModuleTest {
  public static class User {
    private String firstname;

    private String lastname;

    public User(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }

    public String getFirstname() {
      return firstname;
    }

    public String getLastname() {
      return lastname;
    }
  }

  @Test
  public void render() throws Exception {
    PebbleEngine.Builder builder = PebbleModule.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    PebbleTemplateEngine engine = new PebbleTemplateEngine(builder, Collections.singletonList(".peb"));
    MockContext ctx = new MockContext();
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.peb")
            .put("user", new User("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!", output);
  }

  @Test
  public void renderFileSystem() throws Exception {
    PebbleEngine.Builder builder = PebbleModule.create()
        .setTemplatesPath(Paths.get("src", "test", "resources", "views").toString())
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty()));
    PebbleTemplateEngine engine = new PebbleTemplateEngine(builder, Collections.singletonList(".peb"));
    MockContext ctx = new MockContext();
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.peb")
            .put("user", new User("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!", output);
  }
}
