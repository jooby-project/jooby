package jooby.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

import javax.servlet.http.Part;

import jooby.MediaType;
import jooby.Upload;
import jooby.Variant;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

class PartUpload implements Upload {

  private Part part;

  private String workDir;

  private MediaType type;

  private Injector injector;

  private Charset charset;

  public PartUpload(final Injector injector, final Part part, final MediaType type,
      final Charset charset,
      final String workDir) {
    this.injector = injector;
    this.part = part;
    this.charset = charset;
    this.workDir = workDir;
    this.type = type;
  }

  @Override
  public String name() {
    return part.getSubmittedFileName();
  }

  @Override
  public MediaType type() {
    return type;
  }

  @Override
  public Variant header(final String name) {
    Collection<String> headers = part.getHeaders(name);
    return new VariantImpl(injector, name, ImmutableList.copyOf(headers), MediaType.all, charset);
  }

  @Override
  public File file() throws IOException {
    String name = System.currentTimeMillis() + "." + name();
    File fout = new File(workDir, name);
    part.write(fout.getAbsolutePath());
    return fout;
  }

  @Override
  public void close() throws IOException {
    part.delete();
  }

}
