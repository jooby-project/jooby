package jooby;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.function.Supplier;

public class MessageReader {

  interface Binary {
    void read(InputStream in) throws IOException;
  }

  interface Text {
    Object read(Reader reader) throws IOException;
  }

  private Supplier<Reader> reader;

  private Supplier<InputStream> stream;

  public MessageReader(final Supplier<Reader> reader, final Supplier<InputStream> stream) {
    this.reader = requireNonNull(reader, "A reader is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
  }

  @SuppressWarnings("unchecked")
  public <T> T text(final Text text) throws IOException {
    try (Reader reader = this.reader.get()) {
      return (T) text.read(reader);
    }
  }

  public void binary(final Binary bin) throws IOException {
    try (InputStream in = this.stream.get()) {
      bin.read(in);
    }
  }
}
