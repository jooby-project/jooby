package jooby;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public abstract class Response {

  public interface IOSupplier<Out> {
    Out get() throws IOException;
  }

  private MessageConverterSelector selector;

  private List<MediaType> accept;

  private List<MediaType> produces;

  private IOSupplier<Writer> writer;

  private IOSupplier<OutputStream> stream;

  private HttpStatus status = HttpStatus.OK;

  private Multimap<String, String> headers = Multimaps.newListMultimap(new TreeMap<>(
      String.CASE_INSENSITIVE_ORDER), ArrayList::new);

  public Response(final MessageConverterSelector selector,
      final List<MediaType> accept,
      final List<MediaType> produces,
      final IOSupplier<Writer> writer,
      final IOSupplier<OutputStream> stream) {
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.accept = requireNonNull(accept, "The accept are required.");
    this.produces = requireNonNull(produces, "The produces are required.");
    this.writer = requireNonNull(writer, "A writer is required.");
    this.stream = requireNonNull(stream, "A stream is required.");

  }

  public void send(final Object message) throws IOException {
    MessageConverter converter = selector.select(produces)
        .orElseThrow(() -> new HttpException(
            HttpStatus.NOT_ACCEPTABLE,
            produces.stream()
                .map(MediaType::toContentType)
                .collect(Collectors.joining(", "))
            )
        );
    send(message, converter);
  }

  public void send(final Object message, final MessageConverter converter)
      throws IOException {
    requireNonNull(converter, "A converter is required.");
    // dump headers
    headers.entries().stream()
        .filter(it -> !it.getKey().startsWith("@"))
        .forEach(header -> addHeader(header.getKey(), header.getValue()));

    // TODO: setCharSet
    setContentType(converter.types().get(0));
    setStatus(status);

    converter.write(message, new MessageWriter(this.writer, stream),
        ImmutableMultimap.copyOf(headers));
  }

  public HttpStatus status() {
    return status;
  }

  public Response status(final HttpStatus status) {
    this.status = requireNonNull(status, "A status is required.");
    return this;
  }

  public final Response header(final String name, final String value) {
    headers.put(name, value);
    return this;
  }

  protected abstract void setStatus(HttpStatus status);

  protected abstract void setContentType(MediaType contentType);

  protected abstract void addHeader(String name, String value);

}
