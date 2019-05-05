package io.jooby.freemarker;

import com.typesafe.config.ConfigFactory;
import freemarker.template.Configuration;
import io.jooby.Environment;
import io.jooby.MockContext;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreemarkerbyTest {

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
    Configuration freemarker = Freemarkerby.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker);
    MockContext ctx = new MockContext();
    ctx.getAttributes().put("local", "var");
    String output = engine
        .apply(ctx, new ModelAndView("index.ftl")
            .put("user", new User("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!\n", output);
  }
}
