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

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

public enum BuiltinRenderer implements Renderer {

  InputStream {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof InputStream) {
        InputStream in = (InputStream) object;
        try {
          ctx.type(MediaType.octetstream)
              .bytes(out -> ByteStreams.copy(in, out));
        } finally {
          close(in);
        }
      }
    }
  },

  Bytes {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      Class<?> type = object.getClass();
      if (type.isArray() && type.getComponentType() == byte.class) {
        ctx.type(MediaType.octetstream);
        ctx.bytes(out -> ByteStreams.copy(new ByteArrayInputStream((byte[]) object), out));
      }
    }

    @Override
    public String toString() {
      return "byte[]";
    }
  },

  ByteBuffer {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof ByteBuffer) {
        ByteBuffer buffer = (ByteBuffer) object;
        if (buffer.hasArray()) {
          Bytes.render(buffer.array(), ctx);
        } else {
          InputStream.render(new ByteByfferInputStream(buffer), ctx);
        }
      }
    }
  },

  Readable {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof Readable) {
        try {
          Readable in = (Readable) object;
          ctx.type(MediaType.html);
          ctx.text(out -> CharStreams.copy(in, out));
        } finally {
          close(object);
        }
      }
    }
  },

  ToString {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (!(object instanceof View)) {
        ctx.type(MediaType.html);
        ctx.text(out -> out.write(object.toString()));
      }
    }

    @Override
    public String toString() {
      return "toString()";
    }
  };

  static void close(final Object toClose) throws IOException {
    if (toClose instanceof Closeable) {
      ((Closeable) toClose).close();
    }
  }
}
