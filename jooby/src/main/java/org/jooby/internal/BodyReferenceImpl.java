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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.jooby.Parser;

import com.google.common.io.ByteStreams;

public class BodyReferenceImpl implements Parser.BodyReference {

  private Charset charset;

  private long length;

  private File file;

  private byte[] bytes;

  public BodyReferenceImpl(final long length, final Charset charset, final File file,
      final InputStream in, final long bufferSize) throws IOException {
    this.length = length;
    this.charset = charset;
    if (length < bufferSize) {
      bytes = toByteArray(in);
    } else {
      this.file = copy(file, in);
    }
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public byte[] bytes() throws IOException {
    if (bytes == null) {
      return Files.readAllBytes(file.toPath());
    } else {
      return bytes;
    }
  }

  @Override
  public String text() throws IOException {
    return new String(bytes(), charset);
  }

  @Override
  public void writeTo(final OutputStream output) throws IOException {
    if (bytes == null) {
      Files.copy(file.toPath(), output);
    } else {
      output.write(bytes);
    }

  }

  private static byte[] toByteArray(final InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }

  private static File copy(final File file, final InputStream in) throws IOException {
    file.getParentFile().mkdirs();
    copy(in, new FileOutputStream(file));
    return file;
  }

  private static void copy(final InputStream in, final OutputStream out) throws IOException {
    try (InputStream src = in; OutputStream dest = out) {
      ByteStreams.copy(src, dest);
    }
  }

}
