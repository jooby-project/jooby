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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.jooby.Body;
import org.jooby.util.ExSupplier;

import com.google.common.io.Closeables;

public class BodyReaderImpl implements Body.Reader {

  private Charset charset;

  private ExSupplier<InputStream> stream;

  public BodyReaderImpl(final Charset charset, final ExSupplier<InputStream> stream) {
    this.charset = requireNonNull(charset, "A charset is required.");
    this.stream = requireNonNull(stream, "An stream  supplier is required.");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T text(final Text text) throws Exception {
    Reader reader = null;
    try {
      reader = new InputStreamReader(this.stream.get(), charset);
      return (T) text.read(reader);
    } finally {
      Closeables.closeQuietly(reader);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T bytes(final Bytes bin) throws Exception {
    InputStream in = null;
    try {
      in = this.stream.get();
      return (T) bin.read(in);
    } finally {
      Closeables.closeQuietly(in);
    }
  }
}
