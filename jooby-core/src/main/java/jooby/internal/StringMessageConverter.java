package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.List;

import jooby.BodyReader;
import jooby.MediaType;
import jooby.BodyConverter;
import jooby.BodyWriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class StringMessageConverter implements BodyConverter {

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
  public <T> T read(final Class<T> type, final BodyReader reader) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Object message, final BodyWriter writer,
      final Multimap<String, String> headers) throws Exception {
    writer.text(out -> out.write(message.toString()));
  }

}
