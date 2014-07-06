package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.List;

import jooby.BodyReader;
import jooby.MediaType;
import jooby.BodyMapper;
import jooby.BodyWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class ForwardingMessageConverter implements BodyMapper {

  private BodyMapper converter;

  private List<MediaType> types;

  public ForwardingMessageConverter(final BodyMapper converter, final MediaType... types) {
    this.converter = requireNonNull(converter, "The converter is required.");
    this.types = ImmutableList.copyOf(types);
  }

  @Override
  public List<MediaType> types() {
    return types;
  }

  @Override
  public <T> T read(final Class<T> type, final BodyReader reader) throws Exception {
    return converter.read(type, reader);
  }

  @Override
  public void write(final Object message, final BodyWriter writer,
      final Multimap<String, String> headers) throws Exception {
    converter.write(message, writer, headers);
  }

}
