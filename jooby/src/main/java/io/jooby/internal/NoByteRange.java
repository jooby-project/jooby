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
package io.jooby.internal;

import io.jooby.ByteRange;
import io.jooby.Context;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

public class NoByteRange implements ByteRange {
  private long contentLength;

  public NoByteRange(long contentLength) {
    this.contentLength = contentLength;
  }

  @Override public long getStart() {
    return 0;
  }

  @Override public long getEnd() {
    return contentLength;
  }

  @Override public long getContentLength() {
    return contentLength;
  }

  @Nonnull @Override public StatusCode getStatusCode() {
    return StatusCode.OK;
  }

  @Nonnull @Override public String getContentRange() {
    return "bytes */" + contentLength;
  }

  @Nonnull @Override public ByteRange apply(@Nonnull Context ctx) {
    return this;
  }

  @Nonnull @Override public InputStream apply(@Nonnull InputStream input) throws IOException {
    return input;
  }
}
