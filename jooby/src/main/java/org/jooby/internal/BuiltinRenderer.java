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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

public enum BuiltinRenderer implements Renderer {

  Asset {
    @Override
    public void render(final Object value, final Context ctx) throws Exception {
      if (value instanceof Asset) {
        Asset resource = ((Asset) value);
        ctx.type(resource.type())
            .length(resource.length())
            .send(resource.stream());
      }
    }
  },

  InputStream {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof InputStream) {
        InputStream in = (InputStream) object;
        ctx.type(MediaType.octetstream)
            .send(in);
      }
    }
  },

  Reader {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof Reader) {
        ctx.type(MediaType.html)
            .send((Reader) object);
      }
    }
  },

  Bytes {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      Class<?> type = object.getClass();
      if (type.isArray() && type.getComponentType() == byte.class) {
        ctx.type(MediaType.octetstream)
            .send((byte[]) object);
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
        ctx.type(MediaType.octetstream)
            .send(buffer);
      }
    }
  },

  File {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof File) {
        File file = (java.io.File) object;
        ctx.type(MediaType.byFile(file).orElse(MediaType.octetstream));
        ctx.send(new FileInputStream(file));
      }
    }
  },

  CharBuffer {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof CharBuffer) {
        CharBuffer buffer = (CharBuffer) object;
        ctx.type(MediaType.html)
            .send(buffer);
      }
    }
  },

  FileChannel {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (object instanceof FileChannel) {
        ctx.type(MediaType.octetstream);
        ctx.send((FileChannel) object);
      }
    }
  },

  ToString {
    @Override
    public void render(final Object object, final Renderer.Context ctx) throws Exception {
      if (!(object instanceof View)) {
        ctx.type(MediaType.html);
        ctx.send(object.toString());
      }
    }

    @Override
    public String toString() {
      return "toString()";
    }
  };

}
