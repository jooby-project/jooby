package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import jooby.BodyConverter;
import jooby.HttpException;
import jooby.HttpField;
import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.RouteMatcher;
import jooby.ThrowingSupplier;
import jooby.Upload;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public abstract class RequestImpl implements Request {

  private Injector injector;

  private BodyConverterSelector selector;

  private Charset charset;

  private List<MediaType> accept;

  private MediaType contentType;

  private ThrowingSupplier<InputStream> stream;

  private RouteMatcher routeMatcher;

  public RequestImpl(final Injector injector,
      final RouteMatcher routeMatcher,
      final BodyConverterSelector selector,
      final Charset charset,
      final MediaType contentType,
      final List<MediaType> accept,
      final ThrowingSupplier<InputStream> stream) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.routeMatcher = requireNonNull(routeMatcher, "A route matcher is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.contentType = requireNonNull(contentType, "A contentType is required.");
    this.accept = requireNonNull(accept, "An accept is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  @Override
  public String path() {
    return routeMatcher.path();
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
    List<String> values = params(name);
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
    return new GetHeader(name, getHeaders(name));
  }

  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    BodyConverter mapper = selector.forRead(type, Arrays.asList(contentType))
        .orElseThrow(() -> new HttpException(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    return mapper.read(type, new BodyReaderImpl(charset, stream));
  }

  @Override
  public <T> T getInstance(final Key<T> key) {
    return injector.getInstance(key);
  }

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

  private List<String> params(final String name) {
    String var = routeMatcher.vars().get(name);
    if (var != null) {
      return ImmutableList.<String> builder().add(var).addAll(getParams(name)).build();
    }
    return getParams(name);
  }

  protected abstract List<String> getParams(String name);

  protected abstract List<String> getHeaders(String name);

  protected abstract void doDestroy();

}
