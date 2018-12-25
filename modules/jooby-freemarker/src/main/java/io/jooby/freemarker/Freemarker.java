package io.jooby.freemarker;

import freemarker.cache.CacheStorage;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.OutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.jooby.Context;
import io.jooby.Env;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class Freemarker implements TemplateEngine {

  public static class Builder {

    private TemplateLoader loader;

    private Map<String, String> settings = new HashMap<>();

    private CacheStorage cacheStorage;

    private OutputFormat outputFormat = HTMLOutputFormat.INSTANCE;

    public Builder templateLoader(@Nonnull TemplateLoader loader) {
      this.loader = loader;
      return this;
    }

    public Builder setting(@Nonnull String name, @Nonnull String value) {
      this.settings.put(name, value);
      return this;
    }

    public Builder cache(@Nonnull CacheStorage cacheStorage) {
      this.cacheStorage = cacheStorage;
      return this;
    }

    public Builder outputFormat(@Nonnull OutputFormat outputFormat) {
      this.outputFormat = outputFormat;
      return this;
    }

    public Freemarker build() {
      return build(Env.empty("dev"));
    }

    public Freemarker build(@Nonnull Env env) {
      Configuration freemarker = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
      freemarker.setOutputFormat(outputFormat);

      // Settings
      env.get("freemarker").toMap().forEach(settings::putIfAbsent);
      settings.putIfAbsent("defaultEncoding", "UTF-8");
      settings.forEach((k, v) -> {
        try {
          freemarker.setSetting(k, v);
        } catch (TemplateException x) {
          throw Throwing.sneakyThrow(x);
        }
      });

      freemarker.setTemplateLoader(ofNullable(loader).orElseGet(this::defaultTemplateLoader));

      CacheStorage cache = ofNullable(cacheStorage).orElseGet(() ->
          env.matches("dev", "test") ? NullCacheStorage.INSTANCE : null
      );
      if (cache != null) {
        freemarker.setCacheStorage(cache);
      }

      // clear
      this.loader = null;
      this.settings.clear();
      this.settings = null;
      this.cacheStorage = null;
      return new Freemarker(freemarker);
    }

    private TemplateLoader defaultTemplateLoader() {
      return new ClassTemplateLoader(getClass().getClassLoader(), "/views");
    }
  }

  private final Configuration freemarker;

  public Freemarker(Configuration freemarker) {
    this.freemarker = freemarker;
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = freemarker.getTemplate(modelAndView.view);
    StringWriter writer = new StringWriter();
    Map<String, Object> model = new HashMap<>(ctx.locals());
    model.putAll(modelAndView.model);
    template.process(model, writer);
    return writer.toString();
  }

  public static Freemarker.Builder builder() {
    return new Freemarker.Builder();
  }
}
