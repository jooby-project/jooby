package jooby.internal.jetty;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.Part;

import jooby.HttpField;
import jooby.MediaType;
import jooby.Upload;
import jooby.internal.GetHeader;

import com.google.common.collect.ImmutableList;

class PartUpload implements Upload {

  private Part part;

  private String workDir;

  private MediaType type;

  public PartUpload(final Part part, final MediaType type, final String workDir) {
    this.part = part;
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
  public HttpField header(final String name) {
    Collection<String> headers = part.getHeaders(name);
    return new GetHeader(name, ImmutableList.copyOf(headers));
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
