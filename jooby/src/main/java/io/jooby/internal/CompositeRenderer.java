package io.jooby.internal;

import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.Renderer;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;

public class CompositeRenderer implements Renderer {

  private LinkedList<Renderer> list = new LinkedList<>();

  private Renderer templateEngine;

  public CompositeRenderer() {
    list.add(Renderer.TO_STRING);
  }

  public CompositeRenderer add(Renderer renderer) {
    if (renderer instanceof TemplateEngine) {
      templateEngine = computeRenderer(templateEngine, renderer);
    } else {
      list.addFirst(renderer);
    }
    return this;
  }

  private Renderer computeRenderer(Renderer it, Renderer next) {
    if (it == null) {
      return next;
    } else if (it instanceof CompositeRenderer) {
      ((CompositeRenderer) it).list.addFirst(next);
      return it;
    } else {
      CompositeRenderer composite = new CompositeRenderer();
      composite.list.addFirst(it);
      composite.list.addFirst(next);
      return composite;
    }
  }

  @Override public boolean render(@Nonnull Context ctx, @Nonnull Object result) throws Exception {
    if (result instanceof ModelAndView) {
      return templateEngine.render(ctx, result);
    } else {
      Iterator<Renderer> iterator = list.iterator();
      boolean done = false;
      /** NOTE: looks like an infinite loop but there is a default renderer at the end of iterator. */
      while (!done) {
        Renderer next = iterator.next();
        done = next.render(ctx, result);
      }
      return done;
    }
  }
}
