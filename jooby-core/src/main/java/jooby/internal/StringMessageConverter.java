package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;

import jooby.MediaType;
import jooby.MessageConverter;
import jooby.MessageReader;
import jooby.MessageWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class StringMessageConverter implements MessageConverter {

  private List<MediaType> types;

  public StringMessageConverter(final MediaType type) {
    requireNonNull(type, "The type is required.");
    this.types = ImmutableList.of(type);
  }

  @Override
  public List<MediaType> types() {
    return types;
  }

  @Override
  public <T> T read(final Class<T> type, final MessageReader reader) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Object message, final MessageWriter writer,
      final Multimap<String, String> headers) throws IOException {
    writer.text(out -> out.write(message.toString()));
  }

}
