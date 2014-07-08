package jooby;

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jooby.internal.BodyWriterImpl;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public abstract class Response {

  public interface ContentNegotiation {

    public interface Fn {
      Object apply() throws Exception;
    }

    default ContentNegotiation when(final String mediaType, final Fn fn) {
      return when(MediaType.valueOf(mediaType), fn);
    }

    ContentNegotiation when(MediaType mediaType, Fn fn);

    default void send() throws Exception {
      send(null);
    }

    void send(Fn otherwise) throws Exception;
  }

  private BodyConverterSelector selector;

  private Charset charset;

  private List<MediaType> produces;

  private ThrowingSupplier<OutputStream> stream;

  private HttpStatus status = HttpStatus.OK;

  private Multimap<String, String> headers = Multimaps.newListMultimap(new TreeMap<>(
      String.CASE_INSENSITIVE_ORDER), ArrayList::new);

  public Response(final BodyConverterSelector selector,
      final Charset charset,
      final List<MediaType> produces,
      final ThrowingSupplier<OutputStream> stream) {
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.produces = requireNonNull(produces, "Produces are required.");
    this.stream = requireNonNull(stream, "A stream is required.");

  }

  public Charset charset() {
    return charset;
  }

  public Response charset(final Charset charset) {
    this.charset = charset;
    return this;
  }

  public void send(final Object message) throws Exception {
    BodyConverter converter = selector.getOrThrow(produces, HttpStatus.NOT_ACCEPTABLE);
    send(message, converter);
  }

  public void send(final Object message, final BodyConverter converter)
      throws Exception {
    if (message == null) {
      throw new HttpException(HttpStatus.NOT_ACCEPTABLE, Joiner.on(", ").join(produces));
    }
    requireNonNull(converter, "A converter is required.");

    // dump headers
    headers.entries().stream()
        .filter(it -> !it.getKey().startsWith("@"))
        .forEach(header -> addHeader(header.getKey(), header.getValue()));

    setCharset(charset);
    setContentType(converter.types().get(0));
    setStatus(status);

    converter.write(message, new BodyWriterImpl(charset, stream),
        ImmutableMultimap.copyOf(headers));
  }

  public ContentNegotiation when(final String type, final ContentNegotiation.Fn fn) {
    final Map<MediaType, ContentNegotiation.Fn> strategies = new LinkedHashMap<>();
    strategies.put(MediaType.valueOf(type), fn);

    return new ContentNegotiation() {

      @Override
      public ContentNegotiation when(final MediaType mediaType, final Fn fn) {
        strategies.put(mediaType, fn);
        return this;
      }

      @Override
      public void send(final Fn otherwise) throws Exception {
        List<MediaType> filtered = MediaType.matcher(produces).filter(strategies.keySet());
        if (filtered.size() == 0 && otherwise != null) {
          filtered = produces;
        }
        BodyConverter converter = selector.getOrThrow(filtered, HttpStatus.NOT_ACCEPTABLE);
        Fn fn = strategies.get(converter.types().get(0));
        if (fn == null) {
          fn = otherwise;
        }
        if (fn == null) {
          throw new HttpException(HttpStatus.NOT_ACCEPTABLE, Joiner.on(", ").join(filtered));
        }
        Response.this.send(fn.apply());
      }

    };
  }

  public HttpStatus status() {
    return status;
  }

  public Response status(final HttpStatus status) {
    this.status = requireNonNull(status, "A status is required.");
    return this;
  }

  public final Response header(final String name, final String value) {
    headers.put(name, value);
    return this;
  }

  public void reset() {
    headers.clear();
    status = HttpStatus.OK;
    doReset();
  }

  protected abstract void doReset();

  protected abstract void setStatus(HttpStatus status);

  protected abstract void setContentType(MediaType contentType);

  protected abstract void addHeader(String name, String value);

  protected abstract void setCharset(Charset charset);

}
