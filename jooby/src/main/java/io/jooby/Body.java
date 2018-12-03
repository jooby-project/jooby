/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

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
