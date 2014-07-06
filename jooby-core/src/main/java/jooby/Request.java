package jooby;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jooby.internal.BodyReaderImpl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;

public abstract class Request {

  private Injector injector;

  private BodyMapperSelector selector;

  private Charset charset;

  private List<MediaType> accept;

  private MediaType contentType;

  private Map<String, String[]> params;

  private ThrowingSupplier<InputStream> stream;

  public Request(final Injector injector,
      final BodyMapperSelector selector,
      final Charset charset,
      final List<MediaType> accept,
      final MediaType contentType,
      final Map<String, String[]> params,
      final ThrowingSupplier<InputStream> stream) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.charset = requireNonNull(charset, "A charset is required.");
    this.accept = requireNonNull(accept, "An accept is required.");
    this.contentType = requireNonNull(contentType, "A contentType is required.");
    this.params = ImmutableMap.copyOf(requireNonNull(params, "Parameters are required."));
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  public MediaType contentType() {
    return contentType;
  }

  public List<MediaType> accept() {
    return accept;
  }

  public <T> T body(final Class<T> type) throws Exception {
    BodyMapper mapper = selector.getOrThrow(Arrays.asList(contentType),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    return mapper.read(type, new BodyReaderImpl(charset, stream));
  }

  public <T> T get(final Class<T> type) {
    return injector.getInstance(type);
  }

  public String param(final String name) {
    return params.get(name)[0];
  }

  public void destroy() {
    this.selector = null;
    this.contentType = null;
    doDestroy();
  }

  public abstract Optional<String> header(String name);

  public Charset charset() {
    return charset;
  }

  protected abstract void doDestroy();

}
