package jooby;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.Optional;

import jooby.Response.ContentNegotiation.Provider;

public class ForwardingResponse implements Response {

  private Response response;

  public ForwardingResponse(final Response response) {
    this.response = requireNonNull(response, "A response is required.");
  }

  @Override
  public HttpHeader header(final String name) {
    return response.header(name);
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
