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

public class ForwardingMessageConverter implements MessageConverter {

  private MessageConverter converter;

  private List<MediaType> types;

  public ForwardingMessageConverter(final MessageConverter converter, final MediaType... types) {
    this.converter = requireNonNull(converter, "The converter is required.");
    this.types = ImmutableList.copyOf(types);
  }

  @Override
  public List<MediaType> types() {
    return types;
  }

  @Override
  public <T> T read(final Class<T> type, final MessageReader reader) throws IOException {
    return converter.read(type, reader);
  }

  @Override
  public void write(final Object message, final MessageWriter writer,
      final Multimap<String, String> headers) throws IOException {
    converter.write(message, writer, headers);
  }

}
