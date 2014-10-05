package jooby;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import jooby.Response.ContentNegotiation.Provider;

public class ForwardingResponse implements Response {

  private Response response;

  public ForwardingResponse(final Response response) {
    this.response = requireNonNull(response, "A response is required.");
  }

  @Override
  public void download(final String filename, final InputStream stream) throws Exception {
    response.download(filename, stream);
  }

  @Override
  public void download(final String filename, final Reader reader) throws Exception {
    response.download(filename, reader);
  }

  @Override
  public void download(final File file) throws Exception {
    response.download(file);
  }

  @Override
  public void download(final String filename) throws Exception {
    response.download(filename);
  }

  @Override
  public Response cookie(final String name, final String value) {
    return response.cookie(name, value);
  }

  @Override
  public Response cookie(final SetCookie cookie) {
    return response.cookie(cookie);
  }

  @Override
  public Response clearCookie(final String name) {
    return response.clearCookie(name);
  }

  @Override
  public Variant header(final String name) {
    return response.header(name);
  }

  @Override
  public Response header(final String name, final byte value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final char value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final Date value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final double value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final float value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final int value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final long value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final short value) {
    return response.header(name, value);
  }

  @Override
  public Response header(final String name, final String value) {
    return response.header(name, value);
  }

  @Override
  public Charset charset() {
    return response.charset();
  }

  @Override
  public Response charset(final Charset charset) {
    return response.charset(charset);
  }

  @Override
  public <T> T local(final String name) {
    return response.local(name);
  }

  @Override
  public Response local(final String name, final Object value) {
    return response.local(name, value);
  }

  @Override
  public Map<String, Object> locals() {
    return response.locals();
  }

  @Override
  public Optional<MediaType> type() {
    return response.type();
  }

  @Override
  public Response type(final MediaType type) {
    return response.type(type);
  }

  @Override
  public void send(final Object body) throws Exception {
    response.send(body);
  }

  @Override
  public void send(final Object body, final BodyConverter converter) throws Exception {
    response.send(body, converter);
  }

  @Override
  public ContentNegotiation when(final String type, final Provider provider) {
    return response.when(type, provider);
  }

  @Override
  public ContentNegotiation when(final MediaType type, final Provider provider) {
    return response.when(type, provider);
  }

  @Override
  public void redirect(final String location) throws Exception {
    response.redirect(location);
  }

  @Override
  public void redirect(final HttpStatus status, final String location) throws Exception {
    response.redirect(status, location);
  }

  @Override
  public HttpStatus status() {
    return response.status();
  }

  @Override
  public Response status(final HttpStatus status) {
    return response.status(status);
  }

  @Override
  public boolean committed() {
    return response.committed();
  }

}
