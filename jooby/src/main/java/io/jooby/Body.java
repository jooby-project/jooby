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

import io.jooby.internal.FileBody;
import io.jooby.internal.InputStreamBody;
import io.jooby.internal.ByteArrayBody;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;

public interface Body extends Value {

  default String value(Charset charset) {
    return new String(bytes(), charset);
  }

  byte[] bytes();

  boolean isInMemory();

  long getSize();

  ReadableByteChannel channel();

  InputStream stream();

  /* **********************************************************************************************
   * Factory methods:
   * **********************************************************************************************
   */

  static Body empty() {
    return ByteArrayBody.EMPTY;
  }

  static Body of(@Nonnull InputStream stream, long size) {
    return new InputStreamBody(stream, size);
  }

  static Body of(@Nonnull byte[] bytes) {
    return new ByteArrayBody(bytes);
  }

  static Body of(@Nonnull Path file) {
    return new FileBody(file);
  }
}
