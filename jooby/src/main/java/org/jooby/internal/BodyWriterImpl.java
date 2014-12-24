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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

import org.jooby.Body;
import org.jooby.fn.ExSupplier;

public class BodyWriterImpl implements Body.Writer {

  private Charset charset;

  private ExSupplier<OutputStream> stream;

  private ExSupplier<Writer> writer;

  public BodyWriterImpl(final Charset charset,
      final ExSupplier<OutputStream> stream,
      final ExSupplier<Writer> writer) {
    this.charset = requireNonNull(charset, "A charset is required.");
    this.stream = requireNonNull(stream, "A stream is required.");
    this.writer = requireNonNull(writer, "A writer is required.");
  }

  @Override
  public Charset charset() {
    return charset;
  }

  @Override
  public void text(final Text text) throws Exception {
    Writer writer = this.writer.get();
    // don't close on errors
    text.write(uncloseable(writer));
    writer.close();
  }

  @Override
  public void bytes(final Bytes bin) throws Exception {
    OutputStream out = this.stream.get();
    // don't close on errors
    bin.write(uncloseable(out));
    out.flush();
    out.close();
  }

  private static Writer uncloseable(final Writer writer) {
    return new Writer() {

      @Override
      public void write(final char[] cbuf) throws IOException {
        writer.write(cbuf);
      }

      @Override
      public void write(final int c) throws IOException {
        writer.write(c);
      }

      @Override
      public void write(final String str) throws IOException {
        writer.write(str);
      }

      @Override
      public void write(final String str, final int off, final int len) throws IOException {
        super.write(str, off, len);
      }

      @Override
      public void write(final char[] cbuf, final int off, final int len) throws IOException {
        writer.write(cbuf, off, len);
      }

      @Override
      public void flush() throws IOException {
      }

      @Override
      public void close() throws IOException {
      }
    };
  }

  private static OutputStream uncloseable(final OutputStream out) {
    return new OutputStream() {
      @Override
      public void write(final int b) throws IOException {
        out.write(b);
      }

      @Override
      public void write(final byte[] b) throws IOException {
        out.write(b);
      }

      @Override
      public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
      }
    };
  }
}
