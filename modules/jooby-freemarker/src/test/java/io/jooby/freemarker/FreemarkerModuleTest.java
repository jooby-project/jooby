package io.jooby.freemarker;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import freemarker.template.Configuration;
import io.jooby.Environment;
import io.jooby.MockContext;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreemarkerModuleTest {

  public static class MyModel {
    public String firstname;

    public String lastname;

    public MyModel(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }
  }

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
    Configuration freemarker = FreemarkerModule.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker,
        Arrays.asList(".ftl"));
    MockContext ctx = new MockContext();
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.ftl")
            .put("user", new User("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!\n", output);
  }

  @Test
  public void publicField() throws Exception {
    Configuration freemarker = FreemarkerModule.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker,
        Arrays.asList(".ftl"));
    MockContext ctx = new MockContext();
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.ftl")
            .put("user", new MyModel("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!\n", output);
  }

  @Test
  public void customTemplatePath() throws Exception {
    Configuration freemarker = FreemarkerModule.create()
        .build(new Environment(getClass().getClassLoader(),
            ConfigFactory.empty().withValue("templates.path",
                ConfigValueFactory.fromAnyRef("foo"))));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker,
        Arrays.asList(".ftl"));
    MockContext ctx = new MockContext();
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.ftl"));
    assertEquals("var\n", output);
  }
}
