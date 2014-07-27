package jooby.internal;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import jooby.BodyConverter;
import jooby.BodyReader;
import jooby.BodyWriter;
import jooby.MediaType;
import jooby.Viewable;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.inject.TypeLiteral;

public enum FallbackBodyConverter implements BodyConverter {

  COPY_BYTES(MediaType.octetstream) {

    @Override
    public boolean canWrite(final Class<?> type) {
      return InputStream.class.isAssignableFrom(type);
    }

    @Override
    public void write(final Object message, final BodyWriter writer) throws Exception {
      try (InputStream in = (InputStream) message) {
        writer.bytes(out -> ByteStreams.copy(in, out));
      }
    }
  },

  COPY_TEXT(MediaType.html) {

    @Override
    public boolean canWrite(final Class<?> type) {
      return Reader.class.isAssignableFrom(type);
    }

    @Override
    public void write(final Object message, final BodyWriter writer) throws Exception {
      try (Reader in = (Reader) message) {
        writer.text(out -> CharStreams.copy(in, out));
      }
    }
  },

  READ_TEXT(MediaType.text) {

    @Override
    public boolean canRead(final TypeLiteral<?> type) {
      return type.getRawType() == String.class;
    }

    @Override
    public <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
      return reader.text(r -> CharStreams.toString(r));
    }

    @Override
    public boolean canWrite(final Class<?> type) {
      return false;
    }

    @Override
    public void write(final Object message, final BodyWriter writer) throws Exception {
      throw new UnsupportedOperationException();
    }
  },

  TO_HTML(MediaType.html) {

    @Override
    public boolean canWrite(final Class<?> type) {
      return true;
    }

    @Override
    public void write(final Object message, final BodyWriter writer) throws Exception {
      writer.text(out -> out.write(message instanceof Viewable ? ((Viewable) message).model()
          .toString() : message.toString()));
    }
  };

  private final List<MediaType> types;

  private FallbackBodyConverter(final MediaType mediaType) {
    this.types = ImmutableList.of(mediaType);
  }

  @Override
  public boolean canRead(final TypeLiteral<?> type) {
    return false;
  }

  @Override
  public List<MediaType> types() {
    return types;
  }

  @Override
  public <T> T read(final TypeLiteral<T> type, final BodyReader reader) throws Exception {
    throw new UnsupportedOperationException(this.toString() + ".read");
  }

}
