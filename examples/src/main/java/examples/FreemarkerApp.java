/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.freemarker.FreemarkerModule;
import io.jooby.handlebars.HandlebarsModule;

public class FreemarkerApp extends Jooby {

  {
    install(new FreemarkerModule());
    install(new HandlebarsModule());

    get("/freemarker", ctx -> {
      return new ModelAndView("index.ftl").put("name", "Freemarker");
    });

    get("/handlebars", ctx -> {
      return new ModelAndView("index.hbs").put("name", "Handlebars");
    });
  }

  public static void main(String[] args) {
    runApp(args, FreemarkerApp::new);
  }
}
