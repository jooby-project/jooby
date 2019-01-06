/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.utow;

import io.jooby.FileUpload;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.Headers;
import io.jooby.Throwing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UtowFileUpload implements FileUpload {

  private final String name;

  private final FormData.FormValue upload;

  public UtowFileUpload(String name, FormData.FormValue upload) {
    this.name = name;
    this.upload = upload;
  }

  @Override public byte[] bytes() {
    try {
      return Files.readAllBytes(upload.getPath());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public String filename() {
    return upload.getFileName();
  }

  @Override public String contentType() {
    return upload.getHeaders().getFirst(Headers.CONTENT_TYPE);
  }

  @Override public Path path() {
    return upload.getPath();
  }

  @Override public long filesize() {
    return Long.parseLong(upload.getHeaders().getFirst(Headers.CONTENT_LENGTH));
  }

  @Override public void destroy() {
    try {
      Files.deleteIfExists(upload.getPath());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public String name() {
    return name;
  }

  @Override public String toString() {
    return filename();
  }
}
