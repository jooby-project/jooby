package io.jooby.freemarker;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import freemarker.template.Configuration;
import io.jooby.Environment;
import io.jooby.Jooby;
import io.jooby.MockContext;
import io.jooby.ModelAndView;

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
    MockContext ctx = new MockContext()
        .setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.ftl")
            .put("user", new User("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!", output.trim());
  }

  @Test
  public void renderWithLocale() throws Exception {
    Configuration freemarker = FreemarkerModule.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker,
        Arrays.asList(".ftl"));
    MockContext ctx = new MockContext()
        .setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));

    Date nextFriday = java.util.Date
        .from(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.FRIDAY)).atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
        );

    assertEquals("friday", engine.render(ctx, new ModelAndView("locales.ftl")
        .put("someDate", nextFriday)).trim().toLowerCase());

    assertEquals("friday", engine.render(ctx, new ModelAndView("locales.ftl")
        .put("someDate", nextFriday)
        .setLocale(new Locale("en", "GB"))).trim().toLowerCase());

    assertEquals("freitag", engine.render(ctx, new ModelAndView("locales.ftl")
        .put("someDate", nextFriday)
        .setLocale(Locale.GERMAN)).trim().toLowerCase());
  }

  @Test
  public void publicField() throws Exception {
    Configuration freemarker = FreemarkerModule.create()
        .build(new Environment(getClass().getClassLoader(), ConfigFactory.empty(), "test"));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker,
        Arrays.asList(".ftl"));
    MockContext ctx = new MockContext()
        .setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.ftl")
            .put("user", new MyModel("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!", output.trim());
  }

  @Test
  public void customTemplatePath() throws Exception {
    Configuration freemarker = FreemarkerModule.create()
        .build(new Environment(getClass().getClassLoader(),
            ConfigFactory.empty().withValue("templates.path",
                ConfigValueFactory.fromAnyRef("foo"))));
    FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(freemarker,
        Arrays.asList(".ftl"));
    MockContext ctx = new MockContext()
        .setRouter(new Jooby().setLocales(singletonList(Locale.ENGLISH)));
    ctx.getAttributes().put("local", "var");
    String output = engine
        .render(ctx, new ModelAndView("index.ftl"));
    assertEquals("var", output.trim());
  }
}
