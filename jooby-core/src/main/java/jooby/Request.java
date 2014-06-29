package jooby;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;

public abstract class Request {

  private Injector injector;

  private MessageConverterSelector selector;

  private List<MediaType> accept;

  private MediaType contentType;

  private Map<String, String[]> params;

  public Request(final Injector injector,
      final MessageConverterSelector selector,
      final List<MediaType> accept,
      final MediaType contentType,
      final Map<String, String[]> params) {
    this.injector = requireNonNull(injector, "The injector is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.accept = requireNonNull(accept, "The accept is required.");
    this.contentType = requireNonNull(contentType, "A contentType is required.");
    this.params = ImmutableMap.copyOf(requireNonNull(params, "The params are required."));
  }

  public MediaType contentType() {
    return contentType;
  }

  public List<MediaType> accept() {
    return accept;
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

  public abstract Charset charset();

  protected abstract void doDestroy();

}
