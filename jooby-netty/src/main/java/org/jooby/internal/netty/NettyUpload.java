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
package org.jooby.internal.netty;

import io.netty.handler.codec.http.multipart.FileUpload;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.spi.NativeUpload;

import com.google.common.collect.ImmutableList;

public class NettyUpload implements NativeUpload {

  private File file;

  private FileUpload data;

  public NettyUpload(final FileUpload data, final String tmpdir) throws IOException {
    this.data = data;
    String name = "tmp-" + Long.toHexString(System.currentTimeMillis()) + "." + name();
    file = new File(tmpdir, name);
    data.renameTo(file);
  }

  @Override
  public void close() throws IOException {
    file().delete();
    data.delete();
  }

  @Override
  public String name() {
    return data.getFilename();
  }

  private Optional<String> header(final String name) {
    switch (name.toLowerCase()) {
      case "content-transfer-encoding":
        return Optional.of(data.getContentTransferEncoding());
      case "content-disposition":
        return Optional.of("form-data; name=\"" + data.getName() + "\"; filename=\""
            + data.getFilename() + "\"");
      case "content-type":
        Charset charset = data.getCharset();
        if (charset == null) {
          return Optional.ofNullable(data.getContentType());
        }
        return Optional.ofNullable(data.getContentType() + "; charset=" + charset.name());
      default:
        return Optional.empty();
    }
  }

  @Override
  public List<String> headers(final String name) {
    return header(name).<List<String>> map(ImmutableList::of)
        .orElse(Collections.<String> emptyList());
  }

  @Override
  public File file() throws IOException {
    return file;
  }

}
