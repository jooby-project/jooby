package org.jooby.internal;

import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jooby.MediaType;
import org.jooby.MediaType.Matcher;
import org.jooby.Renderer;
import org.jooby.Renderer.Bytes;
import org.jooby.Renderer.Context;
import org.jooby.Renderer.Text;
import org.jooby.util.ExSupplier;

public class RenderContextImpl implements Renderer.Context {

  private ExSupplier<OutputStream> stream;

  private ExSupplier<Writer> writer;

  private Map<String, Object> locals;

  private Matcher matcher;

  private boolean done;

  private Charset charset;

  private Consumer<Long> length;

  private Consumer<MediaType> type;

  private String description;

  public RenderContextImpl(final String description, final ExSupplier<OutputStream> stream,
      final ExSupplier<Writer> writer,
      final Consumer<Long> len, final Consumer<MediaType> type,
      final Map<String, Object> locals, final List<MediaType> produces, final Charset charset) {
    this.description = description;
    this.stream = stream;
    this.writer = writer;
    this.length = len;
    this.type = type;
    this.locals = locals;
    this.matcher = MediaType.matcher(produces);
    this.charset = charset;
  }

  @Override
  public Context length(final long length) {
    this.length.accept(length);
    return this;
  }

  @Override
  public Context type(final MediaType type) {
    this.type.accept(type);
    return this;
  }

  @Override
  public Map<String, Object> locals() {
    return locals;
  }

  @Override
  public boolean accepts(final List<MediaType> types) {
    return matcher.matches(types);
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public void text(final Text text) throws Exception {
    Writer w = this.writer.get();
    // don't close on errors
    text.write(new WriterNoClose(w));
    done = true;
    w.close();
  }

  @Override
  public void bytes(final Bytes bytes) throws Exception {
    OutputStream out = stream.get();
    // don't close on errors
    bytes.write(new OutputStreamNoClose(out));
    done = true;
    out.close();
  }

  public boolean done() {
    return done;
  }

  @Override
  public String toString() {
    return description;
  }

}
