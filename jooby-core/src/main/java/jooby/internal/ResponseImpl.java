package jooby.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jooby.BodyConverter;
import jooby.HttpException;
import jooby.HttpHeader;
import jooby.HttpStatus;
import jooby.MediaType;
import jooby.MediaType.Matcher;
import jooby.Request;
import jooby.Response;
import jooby.Response.ContentNegotiation.Provider;
import jooby.RouteInterceptor;
import jooby.ThrowingSupplier;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;

public abstract class ResponseImpl implements Response {

  private Request request;

  private BodyConverterSelector selector;

  private Set<RouteInterceptor> interceptors;

  private Charset charset;

  private List<MediaType> produces;

  private ThrowingSupplier<OutputStream> stream;

  private HttpStatus status = HttpStatus.OK;

  private MediaType type;

  private ListMultimap<String, String> headers;

  private boolean committed;

  public ResponseImpl(final Request request,
      final BodyConverterSelector selector,
      final Set<RouteInterceptor> interceptors,
      final Charset charset,
      final List<MediaType> produces,
      final ListMultimap<String, String> headers,
      final ThrowingSupplier<OutputStream> stream) {
    this.request = requireNonNull(request, "A request is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.interceptors = requireNonNull(interceptors, "Interceptors are required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.produces = requireNonNull(produces, "Produces are required.");
    this.headers = requireNonNull(headers, "The headers is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  @Override
  public HttpHeader header(final String name) {
    checkArgument(!Strings.isNullOrEmpty(name), "Header's name is missing.");
    return new SetHeader(name, headers.get(name), (values) ->  {
      setHeader(name, values);
    });
  }

  @Override
  public boolean committed() {
    return committed || doCommitted();
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public Response charset(final Charset charset) {
    this.charset = requireNonNull(charset, "A charset is required.");
    return this;
  }

  @Override
  public void send(final Object message) throws Exception {
    requireNonNull(message, "A response message is required.");
    BodyConverter converter = selector.forWrite(message, produces)
        .orElseThrow(() -> new HttpException(HttpStatus.NOT_ACCEPTABLE));
    send(message, converter);
  }

  @Override
  public void send(final Object message, final BodyConverter converter) throws Exception {
    requireNonNull(message, "A response message is required.");
    requireNonNull(converter, "A converter is required.");

    // set default content type if missing
    if (type == null) {
      type(converter.types().get(0));
    }

    if (status == null) {
      status(HttpStatus.OK);
    }

    // fire before send
    for (RouteInterceptor interceptor : interceptors) {
      interceptor.beforeSend(request, this);
    }
    // byte version of http body
    ThrowingSupplier<OutputStream> stream = () -> {
      dumpHeaders();
      return this.stream.get();
    };
    // text version of http body
    ThrowingSupplier<Writer> writer = () -> {
      dumpHeaders();
      setCharset(charset);
      return new OutputStreamWriter(this.stream.get(), charset);
    };
    converter.write(message, new BodyWriterImpl(charset, (h) -> header(h), stream, writer));
  }

  private void dumpHeaders() {
    // dump headers
    headers.entries().stream()
        .filter(it -> !it.getKey().startsWith("@"))
        .forEach(header -> setHeader(header.getKey(), header.getValue()));
  }

  @Override
  public ContentNegotiation when(final String type, final Provider provider) {
    return when(MediaType.valueOf(type), provider);
  }

  @Override
  public ContentNegotiation when(final MediaType type, final ContentNegotiation.Provider provider) {
    final Map<MediaType, ContentNegotiation.Provider> strategies = new LinkedHashMap<>();
    strategies.put(type, provider);

    return new ContentNegotiation() {

      @Override
      public ContentNegotiation when(final MediaType mediaType, final Provider fn) {
        strategies.put(mediaType, fn);
        return this;
      }

      @Override
      public void send() throws Exception {
        List<MediaType> mediaTypes = MediaType.matcher(produces).filter(strategies.keySet());
        if (mediaTypes.size() == 0) {
          throw new HttpException(HttpStatus.NOT_ACCEPTABLE, Joiner.on(", ").join(produces));
        }
        Provider provider = strategies.get(mediaTypes.get(0));
        if (provider == null) {
          Matcher matcher = MediaType.matcher(mediaTypes);
          Optional<MediaType> type = matcher.first(strategies.keySet());
          provider = strategies.get(type.orElseThrow(() -> new HttpException(
              HttpStatus.NOT_ACCEPTABLE, Joiner.on(", ").join(produces))));
        }
        ResponseImpl.this.send(provider.apply());
      }

    };
  }

  @Override
  public HttpStatus status() {
    return status;
  }

  @Override
  public Response status(final HttpStatus status) {
    this.status = requireNonNull(status, "A status is required.");
    this.committed = true;
    setStatus(status);
    return this;
  }

  @Override
  public Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  @Override
  public Response type(final MediaType type) {
    this.type = requireNonNull(type, "Content-Type is required.");
    setContentType(type);
    return this;
  }

  void reset() {
    headers.clear();
    status = null;
    doReset();
  }

  protected abstract void doReset();

  protected abstract boolean doCommitted();

  protected abstract void setStatus(HttpStatus status);

  protected abstract void setContentType(MediaType contentType);

  protected abstract void setHeader(String name, String value);

  protected abstract void setHeader(String name, Iterable<String> values);

  protected abstract void setCharset(Charset charset);

}
