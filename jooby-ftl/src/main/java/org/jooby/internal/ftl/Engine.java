package org.jooby.internal.ftl;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.jooby.Body.Writer;
import org.jooby.View;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

public class Engine implements View.Engine {

  private Configuration freemarker;

  private String prefix;

  private String suffix;

  public Engine(final Configuration freemarker, final String prefix, final String suffix) {
    this.freemarker = requireNonNull(freemarker, "Freemarker config is required.");
    this.prefix = prefix;
    this.suffix = suffix;
  }

  @Override
  public void render(final View viewable, final Writer writer) throws Exception {
    // template
    String name = prefix + viewable.name() + suffix;

    Template template = freemarker.getTemplate(name, writer.charset().name());

    Map<String, Object> hash = new HashMap<>();

    // locals
    hash.putAll(writer.locals());

    // model
    hash.putAll(viewable.model());
    TemplateModel model = new SimpleHash(hash, new FtlWrapper(freemarker.getObjectWrapper()));

    // output
    writer.text(w -> template.process(model, w));
  }

  @Override
  public String name() {
    return "ftl";
  }

  @Override
  public String toString() {
    return name();
  }

}
