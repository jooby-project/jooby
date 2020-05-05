package io.jooby;

import io.jooby.freemarker.FreemarkerModule;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.thymeleaf.ThymeleafModule;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateEngineTest {
  @ServerTest
  public void templateEngines(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new ThymeleafModule());
      app.install(new HandlebarsModule());
      app.install(new FreemarkerModule());

      app.get("/1", ctx -> new ModelAndView("index.hbs").put("name", "Handlebars"));
      app.get("/2", ctx -> new ModelAndView("index.ftl").put("name", "Freemarker"));
      app.get("/3", ctx -> new ModelAndView("index.html").put("name", "Thymeleaf"));
    }).ready(client -> {
      client.get("/1", rsp -> {
        assertEquals("Hello Handlebars!", rsp.body().string().trim());
      });
      client.get("/2", rsp -> {
        assertEquals("Hello Freemarker!", rsp.body().string().trim());
      });
      client.get("/3", rsp -> {
        assertEquals("<!DOCTYPE html>\n"
            + "<html>\n"
            + "<body>\n"
            + "<p>\n"
            + "  Hello <span>Thymeleaf</span>\n"
            + "</p>\n"
            + "</body>\n"
            + "</html>", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void thymeleafShouldReadFromFileSystem(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new ThymeleafModule(runner.resolvePath("src", "test", "resources", "views")));

      app.get("/", ctx -> new ModelAndView("index.html").put("name", "Thymeleaf"));
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("<!DOCTYPE html>\n"
            + "<html>\n"
            + "<body>\n"
            + "<p>\n"
            + "  Hello <span>Thymeleaf</span>\n"
            + "</p>\n"
            + "</body>\n"
            + "</html>", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void handlebarsShouldReadFromFileSystem(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new HandlebarsModule(runner.resolvePath("src", "test", "resources", "views")));

      app.get("/", ctx -> new ModelAndView("index.hbs").put("name", "Handlebars"));
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello Handlebars!", rsp.body().string().trim());
      });
    });
  }

  @ServerTest
  public void freemarkerShouldReadFromFileSystem(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new FreemarkerModule(runner.resolvePath("src", "test", "resources", "views")));

      app.get("/", ctx -> new ModelAndView("index.ftl").put("name", "Freemarker"));
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello Freemarker!", rsp.body().string().trim());
      });
    });
  }
}
