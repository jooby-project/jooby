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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.Renderer;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

public class AssetFormatter implements Renderer {

  @Override
  public void render(final Object object, final Renderer.Context ctx) throws Exception {
    if (object instanceof Asset) {
      Asset asset = (Asset) object;
      MediaType type = asset.type();

      if (type.isText()) {
        ctx.text(to -> {
          Reader from = null;
          try {
            from = new InputStreamReader(asset.stream(), ctx.charset());
            CharStreams.copy(from, to);
          } finally {
            Closeables.closeQuietly(from);
          }
        });
      } else {
        ctx.bytes(to -> {
          InputStream from = null;
          try {
            from = asset.stream();
            ByteStreams.copy(from, to);
          } finally {
            Closeables.closeQuietly(from);
          }
        });
      }
    }
  }
}
