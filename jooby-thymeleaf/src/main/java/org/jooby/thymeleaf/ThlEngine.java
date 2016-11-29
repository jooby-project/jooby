package org.jooby.thymeleaf;

import java.io.FileNotFoundException;
import java.util.Map;

import org.jooby.Env;
import org.jooby.MediaType;
import org.jooby.View;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

class ThlEngine implements View.Engine {

  private final Env env;

  private final TemplateEngine engine;

  public ThlEngine(final TemplateEngine engine, final Env env) {
    this.engine = engine;
    this.env = env;
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void render(final View view, final Context ctx) throws FileNotFoundException, Exception {
    String vname = view.name();

    Map<String, Object> vars = ctx.locals();
    vars.putIfAbsent("_vname", vname);

    Map model = view.model();
    vars.forEach(model::putIfAbsent);
    model.putIfAbsent("xss", new Thlxss(env));

    IContext thlctx = new org.thymeleaf.context.Context(ctx.locale(), model);
    String output = this.engine.process(vname, thlctx);

    ctx.type(MediaType.html)
        .send(output);
  }

  @Override
  public String name() {
    return "thymeleaf";
  }
}
