package org.jooby;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class RequestMock implements Request {

  @Override
  public MediaType type() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<MediaType> accept() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<MediaType> accepts(final List<MediaType> types) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Mutant> params() throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public Mutant param(final String name) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public Mutant header(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Mutant> headers() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Cookie> cookie(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Cookie> cookies() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T body(final TypeLiteral<T> type) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getInstance(final Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Charset charset() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Locale locale() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long length() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String ip() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Route route() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String hostname() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Session session() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Session> ifSession() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String protocol() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean secure() {
    throw new UnsupportedOperationException();
  }

}
