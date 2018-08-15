package io.jooby.internal.netty;

import io.jooby.Value;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import org.jooby.funzy.Throwing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NettyUpload extends Value.Simple implements Value.Upload {

  private final FileUpload upload;
  private Path path;

  public NettyUpload(String name, FileUpload upload) {
    super(name, upload.getFilename());
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
        java.io.File f;
        if (upload.isInMemory()) {
          f = new java.io.File(DiskFileUpload.baseDirectory,
              DiskFileUpload.prefix + System.currentTimeMillis() + DiskFileUpload.postfix);
          upload.renameTo(f);
          upload.release();
        } else {
          f = upload.getFile();
        }
        path = f.toPath();
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
