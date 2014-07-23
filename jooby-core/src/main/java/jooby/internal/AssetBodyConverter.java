package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;

import jooby.Asset;
import jooby.BodyConverter;
import jooby.BodyReader;
import jooby.BodyWriter;
import jooby.MediaType;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

class AssetBodyConverter implements BodyConverter {

  private MediaType mediaType;

  public AssetBodyConverter(final MediaType mediaType) {
    this.mediaType = requireNonNull(mediaType, "A mediaType is required.");
  }

  @Override
  public boolean canRead(final Type type) {
    return false;
  }

  @Override
  public boolean canWrite(final Class<?> type) {
    return Asset.class.isAssignableFrom(type);
  }

  @Override
  public List<MediaType> types() {
    return ImmutableList.of(mediaType);
  }

  @Override
  public <T> T read(final Class<T> type, final BodyReader reader) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(final Object message, final BodyWriter writer) throws Exception {
    Asset asset = (Asset) message;
    boolean text = mediaType.equals(MediaType.javascript) || mediaType.equals(MediaType.css)
        || mediaType.equals(MediaType.json) || MediaType.text.matches(mediaType);
    if (text) {
      writer.text(to -> {
        try (Reader from = new InputStreamReader(asset.stream(), writer.charset())) {
          CharStreams.copy(from, to);
        }
      });
    } else {
      writer.bytes(to -> {
        try (InputStream from = asset.stream()) {
          ByteStreams.copy(from, to);
        }
      });
    }
  }
}
