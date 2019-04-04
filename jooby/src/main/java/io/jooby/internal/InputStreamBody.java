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
package io.jooby.internal;

import io.jooby.Body;
import io.jooby.ServerOptions;
import io.jooby.Throwing;
import io.jooby.Value;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class InputStreamBody implements Body {
  private long length;
  private InputStream in;

  public InputStreamBody(InputStream stream, long contentLength) {
    this.in = stream;
    this.length = contentLength;
  }

  public byte[] bytes() {
    try (InputStream stream = in) {
      int bufferSize = ServerOptions._16KB;
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

  @Override public boolean isInMemory() {
    return false;
  }

  @Override public ReadableByteChannel channel() {
    return Channels.newChannel(in);
  }

  @Override public long getLength() {
    return length;
  }

  @Override public InputStream stream() {
    return in;
  }

  @Nonnull @Override public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @Nonnull @Override public Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return new Missing(name);
  }

  @Override public String name() {
    return "body";
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
