package io.jooby.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Hbs implements TemplateEngine {

  private Handlebars handlebars;

  public Hbs(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  public Hbs() {
    this(build(new ClassPathTemplateLoader("/views", "")));
  }

  public static Handlebars build(TemplateLoader loader) {
    Handlebars handlebars = new Handlebars(loader);
    handlebars.setCharset(StandardCharsets.UTF_8);
    return handlebars;
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = handlebars.compile(modelAndView.view);
    Map<String, Object> model = new HashMap<>(ctx.locals());
    model.putAll(modelAndView.model);
    return template.apply(model);
  }
}
