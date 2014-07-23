package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import jooby.BodyConverter;
import jooby.Cookie;
import jooby.HttpException;
import jooby.HttpField;
import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.ThrowingSupplier;
import jooby.Upload;

import com.google.common.collect.ListMultimap;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public abstract class RequestImpl implements Request {

  private Injector injector;

  private BodyConverterSelector selector;

  private Charset charset;

  private List<MediaType> accept;

  private MediaType contentType;

  private ListMultimap<String, String> params;

  private ThrowingSupplier<InputStream> stream;

  private ListMultimap<String, String> headers;

  private List<Cookie> cookies;

  private String path;

  public RequestImpl(final Injector injector,
      final String path,
      final BodyConverterSelector selector,
      final Charset charset,
      final List<MediaType> accept,
      final MediaType contentType,
      final ListMultimap<String, String> params,
      final ListMultimap<String, String> headers,
      final List<Cookie> cookies,
      final ThrowingSupplier<InputStream> stream) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.path = requireNonNull(path, "Request path is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.accept = requireNonNull(accept, "An accept is required.");
    this.contentType = requireNonNull(contentType, "A contentType is required.");
    this.params = requireNonNull(params, "Parameters are required.");
    this.headers = requireNonNull(headers, "Headers are required.");
    this.cookies = requireNonNull(cookies, "The cookies is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public MediaType contentType() {
    return contentType;
  }

  @Override
  public List<MediaType> accept() {
    return accept;
  }

  @Override
  public HttpField param(final String name) throws Exception {
    requireNonNull(name, "Parameter's name is missing.");
    List<String> values = params.get(name);
    if (values.isEmpty()) {
      List<Upload> files = uploads(name);
      if (files != null && files.size() > 0) {
        return new GetUpload(name, files);
      }
    }
    return new GetterImpl(name, values);
  }

  @Override
  public HttpField header(final String name) {
    requireNonNull(name, "Header's name is missing.");
    return new GetHeader(name, headers.get(name));
  }

  @Override
  public Cookie cookie(final String name) {
    return cookies.stream().filter(c -> c.name().equals(name)).findFirst().orElse(null);
  }

  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    BodyConverter mapper = selector.forRead(type, Arrays.asList(contentType))
        .orElseThrow(() -> new HttpException(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    return mapper.read(type, new BodyReaderImpl(charset, stream));
  }

  @Override
  public <T> T get(final Class<T> type) {
    return injector.getInstance(type);
  }

  @Override
  public <T> T get(final Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public void destroy() {
    this.selector = null;
    this.contentType = null;
    doDestroy();
  }

  @Override
  public Charset charset() {
    return charset;
  }

  protected List<Upload> uploads(final String name) throws Exception {
    if (!contentType().name().startsWith(MediaType.multipart.name())) {
      return null;
    }
    return getUploads(name);
  }

  protected abstract List<Upload> getUploads(final String name) throws Exception;

  protected abstract void doDestroy();

}
