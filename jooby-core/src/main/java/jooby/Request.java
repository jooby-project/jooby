package jooby;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jooby.internal.BodyReaderImpl;
import jooby.internal.mvc.ParamParser;

import com.google.common.collect.ListMultimap;
import com.google.inject.Injector;

public abstract class Request {

  private Injector injector;

  private BodyConverterSelector selector;

  private Charset charset;

  private List<MediaType> accept;

  private MediaType contentType;

  private ListMultimap<String, String> params;

  private ThrowingSupplier<InputStream> stream;

  private ListMultimap<String, String> headers;

  private List<Cookie> cookies;

  public Request(final Injector injector,
      final BodyConverterSelector selector,
      final Charset charset,
      final List<MediaType> accept,
      final MediaType contentType,
      final ListMultimap<String, String> params,
      final ListMultimap<String, String> headers,
      final List<Cookie> cookies,
      final ThrowingSupplier<InputStream> stream) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.accept = requireNonNull(accept, "An accept is required.");
    this.contentType = requireNonNull(contentType, "A contentType is required.");
    this.params = requireNonNull(params, "Parameters are required.");
    this.headers = requireNonNull(headers, "Headers are required.");
    this.cookies = requireNonNull(cookies, "The cookies is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  public MediaType contentType() {
    return contentType;
  }

  public List<MediaType> accept() {
    return accept;
  }

  @SuppressWarnings("unchecked")
  public <T> T param(final String name, final Type type) throws Exception {
    ParamParser parser = ParamParser.parser(type)
        .orElseThrow(() -> new IllegalArgumentException("No parser found for: " + type
            .getTypeName()));
    return (T) parser.parse(type, params(name));
  }

  public List<String> params(final String name) {
    List<String> values = params.get(name);
    return values == null || values.size() == 0 ? null : values;
  }

  public String param(final String name) {
    List<String> values = params.get(name);
    return values == null || values.size() == 0 ? null : values.get(0);
  }

  @SuppressWarnings("unchecked")
  public <T> T header(final String name, final Type type) throws Exception {
    ParamParser parser = ParamParser.parser(type)
        .orElseThrow(() -> new IllegalArgumentException("No parser found for: " + type
            .getTypeName()));
    List<String> values = headers(name);
    return (T) parser.parse(type, values);
  }

  public String header(final String name) {
    Collection<String> collection = headers.get(name);
    if (collection == null || collection.size() == 0) {
      return null;
    }
    return collection.iterator().next();
  }

  public List<String> headers(final String name) {
    List<String> values = headers.get(name);
    return values == null || values.size() == 0 ? null : values;
  }

  public Cookie cookie(final String name) {
    return cookies.stream().filter(c -> c.name().equals(name)).findFirst().orElse(null);
  }

  public <T> T body(final Class<T> type) throws Exception {
    BodyConverter mapper = selector.getOrThrow(Arrays.asList(contentType),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    return mapper.read(type, new BodyReaderImpl(charset, stream));
  }

  public <T> T get(final Class<T> type) {
    return injector.getInstance(type);
  }

  public void destroy() {
    this.selector = null;
    this.contentType = null;
    doDestroy();
  }

  public Charset charset() {
    return charset;
  }

  protected abstract void doDestroy();

}
