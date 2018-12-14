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
package io.jooby.internal.netty;

import io.jooby.FileUpload;
import io.jooby.Value;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.jooby.Throwing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NettyFileUpload extends Value.Simple implements FileUpload {

  private final io.netty.handler.codec.http.multipart.FileUpload upload;
  private final Path basedir;
  private Path path;

  public NettyFileUpload(Path basedir, String name, io.netty.handler.codec.http.multipart.FileUpload upload) {
    super(name, upload.getFilename());
    this.basedir = basedir;
    this.upload = upload;
  }

  @Override public String filename() {
    return upload.getFilename();
  }

  @Override public String contentType() {
    return upload.getContentType();
  }

  @Override public long filesize() {
    return upload.length();
  }

  @Override public Path path() {
    try {
      if (path == null) {
        if (upload.isInMemory()) {
          path = basedir
              .resolve(DiskFileUpload.prefix + System.currentTimeMillis()
                  + DiskFileUpload.postfix);
          upload.renameTo(path.toFile());
          upload.release();
        } else {
          path = upload.getFile().toPath();
        }
      }
      return path;
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public void destroy() {
    try {
      if (upload.refCnt() > 0) {
        upload.release();
      }
      if (path != null) {
        Files.deleteIfExists(path);
      }

    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
