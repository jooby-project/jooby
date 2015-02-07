/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.common.io.Closeables;

/**
 * Like {@link java.io.ByteArrayOutputStream} but with access to the internal buffer.
 *
 * @author edgar
 */
public class FastByteArrayOutputStream extends ByteArrayOutputStream {

  private BiConsumer<String, String> headers;

  private Supplier<OutputStream> stream;

  public FastByteArrayOutputStream(final Supplier<OutputStream> stream,
      final BiConsumer<String, String> headers) {
    this(stream, headers, 1024);
  }

  public FastByteArrayOutputStream(final Supplier<OutputStream> stream,
      final BiConsumer<String, String> headers, final int size) {
    super(size);
    this.stream = stream;
    this.headers = headers;
    buf = new byte[size];
  }

  @Override
  public void close() throws IOException {
    headers.accept("Content-Length", count + "");
    OutputStream out = null;
    try {
      out = stream.get();
      out.write(buf, 0, count);
    } finally {
      Closeables.close(out, true);
    }
  }

}
