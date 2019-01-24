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
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface Sender {

  interface Callback {
    void onComplete(Context ctx, Throwable cause);
  }

  default Sender sendString(@Nonnull String data, @Nonnull Callback callback) {
    return sendString(data, StandardCharsets.UTF_8, callback);
  }

  default Sender sendString(@Nonnull String data, Charset charset, @Nonnull Callback callback) {
    return sendBytes(data.getBytes(charset), callback);
  }

  Sender sendBytes(@Nonnull byte[] data, @Nonnull Callback callback);

  void close();
}
