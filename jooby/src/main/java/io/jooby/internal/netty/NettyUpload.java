package io.jooby.internal.netty;

import io.jooby.Upload;
import io.jooby.Value;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import org.jooby.funzy.Throwing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NettyUpload extends Value.Simple implements Upload {

  private final FileUpload upload;
  private final Path basedir;
  private Path path;

  public NettyUpload(Path basedir, String name, FileUpload upload) {
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
              .resolve(DiskFileUpload.prefix + System.currentTimeMillis() + DiskFileUpload.postfix);
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
