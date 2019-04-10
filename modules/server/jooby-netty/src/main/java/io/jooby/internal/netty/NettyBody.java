/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.Body;
import io.jooby.Throwing;
import io.jooby.Value;
import io.jooby.internal.MissingValue;
import io.netty.handler.codec.http.multipart.HttpData;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NettyBody implements Body {
  private final HttpData data;
  private long length;

  public NettyBody(HttpData data, long contentLength) {
    this.data = data;
    this.length = contentLength;
  }

  @Override public boolean isInMemory() {
    return data.isInMemory();
  }

  @Override public long getSize() {
    return length;
  }

  @Override public InputStream stream() {
    try {
      if (data.isInMemory()) {
        return new ByteArrayInputStream(data.get());
      }
      return new FileInputStream(data.getFile());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public ReadableByteChannel channel() {
    return Channels.newChannel(stream());
  }

  @Override public byte[] bytes() {
    try {
      if (data.isInMemory()) {
        return data.get();
      }
      return Files.readAllBytes(data.getFile().toPath());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public String value() {
    return value(StandardCharsets.UTF_8);
  }

  @Nonnull @Override public Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Nonnull @Override public Value get(@Nonnull String name) {
    return new MissingValue(name);
  }

  @Override public String name() {
    return "body";
  }

  @Override public Map<String, List<String>> toMultimap() {
    return Collections.emptyMap();
  }
}
