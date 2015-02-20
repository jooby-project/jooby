package org.jooby.internal.netty;

import io.netty.handler.codec.http.multipart.FileUpload;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.MediaType;
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

  @Override
  public Optional<String> header(final String name) {
    switch (name.toLowerCase()) {
      case "content-length":
        return Optional.of(Long.toString(data.length()));
      case "content-type":
        String contentType = data.getContentType();
        if (contentType == null) {
          return Optional.of(MediaType.octetstream.name());
        }
        Charset charset = data.getCharset();
        if (charset == null) {
          return Optional.of(contentType);
        }
        return Optional.of(contentType + "; charset=" + charset.name());
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
