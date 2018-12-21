package io.jooby.freemarker;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;
import io.jooby.Throwing;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Freemarker implements TemplateEngine {

  private final Configuration freemarker;

  public Freemarker(Configuration freemarker) {
    this.freemarker = freemarker;
  }

  public Freemarker() {
    this(create(new ClassTemplateLoader(Freemarker.class.getClassLoader(), "/views"), options()));
  }

  public static Properties options() {
    Properties properties = new Properties();
    properties.setProperty("object_wrapper", "default");
    properties.setProperty("template_exception_handler", "default");
    properties.setProperty("defaultEncoding", "UTF-8");
    return properties;
  }

  public static Configuration create(TemplateLoader loader, Properties options) {
    try {
      Configuration freemarker = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
      freemarker.setSettings(options);
      freemarker.setTemplateLoader(loader);
      freemarker.setOutputFormat(HTMLOutputFormat.INSTANCE);
      return freemarker;
    } catch (TemplateException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = freemarker.getTemplate(modelAndView.view);
    StringWriter writer = new StringWriter();
    Map<String, Object> model = new HashMap<>(ctx.locals());
    model.putAll(modelAndView.model);
    template.process(model, writer);
    return writer.toString();
  }
}
