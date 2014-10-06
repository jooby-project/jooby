package jooby;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class ForwardingRequest implements Request {

  private Request request;

  public ForwardingRequest(final Request request) {
    this.request = requireNonNull(request, "A HTTP request is required.");
  }

  @Override
  public String path() {
    return request.path();
  }

  @Override
  public MediaType type() {
    return request.type();
  }

  @Override
  public List<MediaType> accept() {
    return request.accept();
  }

  @Override
  public Optional<MediaType> accepts(final List<MediaType> types) {
    return request.accepts(types);
  }

  @Override
  public Map<String, Variant> params() throws Exception {
    return request.params();
  }

  @Override
  public Variant param(final String name) throws Exception {
    return request.param(name);
  }

  @Override
  public Variant header(final String name) {
    return request.header(name);
  }

  @Override
  public Map<String, Variant> headers() {
    return request.headers();
  }

  @Override
  public Cookie cookie(final String name) {
    return request.cookie(name);
  }

  @Override
  public List<Cookie> cookies() {
    return request.cookies();
  }

  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    return request.body(type);
  }

  @Override
  public <T> T getInstance(final Key<T> key) {
    return request.getInstance(key);
  }

  @Override
  public Charset charset() {
    return request.charset();
  }

  @Override
  public String ip() {
    return request.ip();
  }

  @Override
  public Route route() {
    return request.route();
  }

  @Override
  public String hostname() {
    return request.hostname();
  }

  @Override
  public String protocol() {
    return request.protocol();
  }

  @Override
  public Optional<MediaType> accepts(final MediaType... types) {
    return request.accepts(types);
  }

  @Override
  public Optional<MediaType> accepts(final String... types) {
    return request.accepts(types);
  }

  @Override
  public <T> T body(final Class<T> type) throws Exception {
    return request.body(type);
  }

  @Override
  public <T> T getInstance(final Class<T> type) {
    return request.getInstance(type);
  }

  @Override
  public <T> T getInstance(final TypeLiteral<T> type) {
    return request.getInstance(type);
  }

  @Override
  public boolean secure() {
    return request.secure();
  }

  public Request delegate() {
    return request;
  }

  @Override
  public String toString() {
    return request.toString();
  }
}
