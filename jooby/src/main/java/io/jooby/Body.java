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

import io.jooby.internal.BodyImpl;
import io.jooby.internal.ByteArrayBody;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public interface Body extends Value {

  default String value(Charset charset) {
    return new String(bytes(), charset);
  }

  default byte[] bytes() {
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

  static Body empty() {
    return ByteArrayBody.EMPTY;
  }

  static Body of(@Nonnull InputStream stream, long contentLength) {
    return new BodyImpl(stream, contentLength);
  }

  static Body of(@Nonnull byte[] bytes) {
    return new ByteArrayBody(bytes);
  }
}
