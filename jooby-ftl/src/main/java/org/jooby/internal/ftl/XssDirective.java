package org.jooby.internal.ftl;

import java.util.List;
import java.util.stream.Collectors;

import org.jooby.Env;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import javaslang.control.Try;

public class XssDirective implements TemplateMethodModelEx {

  private Env env;

  public XssDirective(final Env env) {
    this.env = env;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public Object exec(final List arguments) throws TemplateModelException {
    List<String> args = (List<String>) arguments.stream()
        .map(it -> Try.of(() -> ((TemplateScalarModel) it).getAsString()).get())
        .collect(Collectors.toList());
    String[] xss = args.subList(1, args.size())
        .toArray(new String[arguments.size() - 1]);
    return env.xss(xss).apply(args.get(0));
  }

}
