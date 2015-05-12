package org.jooby.internal;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Status;
import org.jooby.util.ExSupplier;

import com.google.common.base.Joiner;

public class RendererExecutor {

  private Set<Renderer> renderers;

  @Inject
  public RendererExecutor(final Set<Renderer> renderers) {
    this.renderers = renderers;
  }

  public void render(final String path, final Object value,
      final ExSupplier<OutputStream> stream,
      final ExSupplier<Writer> writer,
      final Consumer<Long> length,
      final Consumer<MediaType> type,
      final Map<String, Object> locals,
      final List<MediaType> produces,
      final Charset charset) throws Exception {

    RenderContextImpl ctx = new RenderContextImpl(path + " " + renderers, stream, writer, length,
        type, locals, produces, charset);
    Iterator<Renderer> it = renderers.iterator();
    while (!ctx.done()) {
      if (it.hasNext()) {
        Renderer next = it.next();
        next.render(value, ctx);
      } else {
        throw new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(produces));
      }
    }

  }

}
