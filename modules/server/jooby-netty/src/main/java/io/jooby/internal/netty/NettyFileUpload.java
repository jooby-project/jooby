/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.FileUpload;
import io.jooby.Sneaky;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.multipart.DiskFileUpload;

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
      throw Sneaky.propagate(x);
    }
  }

  @Override public InputStream stream() {
    try {
      if (upload.isInMemory()) {
        return new ByteBufInputStream(upload.content(), true);
      }
      return Files.newInputStream(path());
    } catch (IOException x) {
      throw Sneaky.propagate(x);
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
      throw Sneaky.propagate(x);
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
      throw Sneaky.propagate(x);
    }
  }

  @Override public String toString() {
    return getFileName();
  }
}
