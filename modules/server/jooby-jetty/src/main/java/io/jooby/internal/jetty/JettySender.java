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
package io.jooby.internal.jetty;

import io.jooby.Sender;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;

public class JettySender implements Sender {
  private final JettyContext ctx;
  private final HttpOutput sender;

  public JettySender(JettyContext ctx, HttpOutput sender) {
    this.ctx = ctx;
    this.sender = sender;
  }

  @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
    try {
      sender.write(data);
      sender.flush();
      callback.onComplete(ctx, null);
    } catch (IOException e) {
      callback.onComplete(ctx, e);
    }
    return this;
  }

  @Override public void close() {
    ctx.destroy(null);
  }

}
