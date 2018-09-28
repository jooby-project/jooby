package io.jooby;

import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Body {

  default String string() {
    return string(StandardCharsets.UTF_8);
  }

  default String string(Charset charset) {
    return new String(bytes(), charset);
  }

  default byte[] bytes() {
    // TODO: Improve reading for small bodies
    try (InputStream stream = stream()) {
      int bufferSize = Server._16KB;
      ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
      int len;
      byte[] buffer = new byte[bufferSize];
      while ((len = stream.read(buffer, 0, buffer.length)) != -1) {
        out.write(buffer, 0, len);
      }
      return out.toByteArray();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  long contentLength();

  InputStream stream();

  static Body of(@Nonnull InputStream stream, long contentLength) {
    return new Body() {
      @Override public long contentLength() {
        return contentLength;
      }

      @Override public InputStream stream() {
        return stream;
      }
    };
  }
}
