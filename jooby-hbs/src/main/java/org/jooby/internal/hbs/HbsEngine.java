package org.jooby.internal.hbs;

import static java.util.Objects.requireNonNull;

import org.jooby.Body;
import org.jooby.View;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;

public class HbsEngine implements View.Engine {

  private Handlebars handlebars;

  private ValueResolver[] resolvers;

  public HbsEngine(final Handlebars handlebars, final ValueResolver[] resolvers) {
    this.handlebars = requireNonNull(handlebars, "Handlebars is required.");
    this.resolvers = requireNonNull(resolvers, "Resolvers are required.");
  }

  @Override
  public String name() {
    return "hbs";
  }

  @Override
  public void render(final View view, final Body.Writer writer) throws Exception {
    Template template = handlebars.compile(view.name());

    Context context = Context
        .newBuilder(view.model())
        // merge request locals (req+sessions locals)
        .combine(writer.locals())
        .resolver(resolvers)
        .build();

    // rendering it
    writer.text(out -> template.apply(context, out));
  }

  @Override
  public String toString() {
    return name();
  }

}
