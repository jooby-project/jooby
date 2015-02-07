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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.View;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.inject.TypeLiteral;

public class BuiltinBodyConverter {

  public static Body.Formatter formatStream = new Body.Formatter() {
    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.octetstream);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return InputStream.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final Body.Writer writer) throws Exception {
      InputStream in = (InputStream) body;
      try {
        writer.bytes(out -> ByteStreams.copy(in, out));
      } finally {
        Closeables.closeQuietly(in);
      }
    }

    @Override
    public String toString() {
      return "Formatter for: " + InputStream.class.getName();
    }
  };

  public static Body.Formatter formatByteArray = new Body.Formatter() {
    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.octetstream);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return type.isArray() && type.getComponentType() == byte.class;
    }

    @Override
    public void format(final Object body, final Body.Writer writer) throws Exception {
      try (InputStream in = new ByteArrayInputStream((byte[]) body)) {
        writer.bytes(out -> ByteStreams.copy(in, out));
      }
    }

    @Override
    public String toString() {
      return "Formatter for: byte[]";
    }
  };

  public static Body.Formatter formatByteBuffer = new Body.Formatter() {
    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.octetstream);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final Body.Writer writer) throws Exception {
      ByteBuffer buffer = (ByteBuffer) body;
      if (buffer.hasArray()) {
        formatByteArray.format(buffer.array(), writer);
      } else {
        ByteBuffer readonly = buffer.asReadOnlyBuffer();
        if (readonly.position() > 0) {
          readonly.rewind();
        }
        try (InputStream in = toInputStream(readonly)) {
          writer.bytes(out -> ByteStreams.copy(in, out));
        }
      }
    }

    @Override
    public String toString() {
      return "Formatter for: " + ByteBuffer.class.getName();
    }
  };

  public static Body.Formatter formatReader = new Body.Formatter() {

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.html);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return Readable.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final Body.Writer writer) throws Exception {
      try {
        Readable in = (Readable) body;
        writer.text(out -> CharStreams.copy(in, out));
      } finally {
        if (body instanceof Closeable) {
          Closeables.close((Closeable) body, true);
        }
      }
    }

    @Override
    public String toString() {
      return "Formatter for: " + Readable.class.getName();
    }
  };

  public static Body.Formatter formatAny = new Body.Formatter() {

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.html);
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return !View.class.isAssignableFrom(type);
    }

    @Override
    public void format(final Object body, final Body.Writer writer) throws Exception {
      writer.text(out -> out.write(body.toString()));
    }

    @Override
    public String toString() {
      return "Formatter for: Object.toString()";
    }
  };

  public static Body.Parser parseString = new Body.Parser() {

    @Override
    public List<MediaType> types() {
      return ImmutableList.of(MediaType.plain);
    }

    @Override
    public boolean canParse(final TypeLiteral<?> type) {
      return CharSequence.class.isAssignableFrom(type.getRawType());
    }

    @Override
    public <T> T parse(final TypeLiteral<T> type, final Body.Reader reader) throws Exception {
      return reader.text(r -> CharStreams.toString(r));
    }

    @Override
    public String toString() {
      return "Parser for: " + CharSequence.class.getName();
    }
  };

  protected static InputStream toInputStream(final ByteBuffer buffer) {
    return new InputStream() {

      @Override
      public int available() {
        return buffer.remaining();
      }

      @Override
      public int read() throws IOException {
        return buffer.hasRemaining() ? buffer.get() & 0xFF : -1;
      }

      @Override
      public int read(final byte[] bytes, final int off, final int len) throws IOException {
        if (!buffer.hasRemaining()) {
          return -1;
        }
        int count = Math.min(len, buffer.remaining());
        buffer.get(bytes, off, count);
        return count;
      }
    };
  }
}
