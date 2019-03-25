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
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.jooby.Throwing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class NettyFileUpload implements FileUpload {

  private final io.netty.handler.codec.http.multipart.FileUpload upload;
  private final Path basedir;
  private final String name;
  private Path path;

  public NettyFileUpload(Path basedir, String name,
      io.netty.handler.codec.http.multipart.FileUpload upload) {
    this.name = name;
    this.basedir = basedir;
    this.upload = upload;
  }

  @Override public String name() {
    return name;
  }

  @Override public byte[] bytes() {
    try {
      if (upload.isInMemory()) {
        return upload.get();
      }
      return Files.readAllBytes(path());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public InputStream stream() {
    try {
      if (upload.isInMemory()) {
        return new ByteBufInputStream(upload.content(), true);
      }
      return Files.newInputStream(path());
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public String getFileName() {
    return upload.getFilename();
  }

  @Override public String getContentType() {
    return upload.getContentType();
  }

  @Override public long getFileSize() {
    return upload.length();
  }

  @Override public Path path() {
    try {
      if (path == null) {
        if (upload.isInMemory()) {
          path = basedir
              .resolve(DiskFileUpload.prefix + System.nanoTime() + DiskFileUpload.postfix);
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

  @Override public String toString() {
    return getFileName();
  }
}
