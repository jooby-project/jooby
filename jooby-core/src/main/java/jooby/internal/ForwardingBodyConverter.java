package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.List;

import jooby.BodyConverter;
import jooby.BodyReader;
import jooby.BodyWriter;
import jooby.MediaType;

import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;

public class ForwardingBodyConverter implements BodyConverter {

  private BodyConverter converter;

  private List<MediaType> types;

  public ForwardingBodyConverter(final BodyConverter converter, final MediaType... types) {
    this.converter = requireNonNull(converter, "The converter is required.");
    this.types = ImmutableList.copyOf(types);
  }

  @Override
  public boolean canRead(final TypeLiteral<?> type) {
    return converter.canRead(type);
  }

  @Override
  public boolean canWrite(final Class<?> type) {
    return converter.canWrite(type);
  }

  public BodyConverter converter() {
    return converter;
  }

  @Override
  public List<MediaType> types() {
    return types;
  }

  @Override
  public <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
    return converter.read(type, reader);
  }

  @Override
  public void write(final Object body, final BodyWriter writer) throws Exception {
    converter.write(body, writer);
  }

}
