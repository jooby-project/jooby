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
import java.nio.ByteBuffer;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.View;

public enum BuiltinRenderer implements Renderer {

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
        ctx.send(new FileInputStream((File) object));
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
