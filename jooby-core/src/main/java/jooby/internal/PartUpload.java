package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import javax.servlet.http.Part;

import jooby.MediaType;
import jooby.Upload;
import jooby.Variant;

import com.google.common.collect.ImmutableList;
import com.google.inject.spi.TypeConverterBinding;

class PartUpload implements Upload {

  private Part part;

  private String workDir;

  private MediaType type;

  private Set<TypeConverterBinding> typeConverters;

  public PartUpload(final Part part, final MediaType type, final String workDir
      , final Set<TypeConverterBinding> typeConverters) {
    this.part = part;
    this.workDir = workDir;
    this.type = type;
    this.typeConverters = requireNonNull(typeConverters, "Type converters are required.");
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
    return new VariantImpl(name, ImmutableList.copyOf(headers), typeConverters);
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
