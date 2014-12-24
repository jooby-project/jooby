package org.jooby.internal.undertow;

import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.Headers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Upload;
import org.jooby.internal.MutantImpl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

public class UndertowUpload implements Upload {

  private Injector injector;

  private FormValue value;

  private Supplier<MediaType> type;

  private Charset charset;

  public UndertowUpload(final Injector injector, final FormValue value, final Charset charset) {
    this.injector = injector;
    this.value = value;
    this.charset = charset;
    this.type = Suppliers.memoize(
        () -> Optional
            .ofNullable(value.getHeaders())
            .map(headers ->
                Optional.ofNullable(headers.get(Headers.CONTENT_TYPE))
                    .map(h -> MediaType.valueOf(h.getFirst()))
                    .orElse(MediaType.byPath(value.getFileName())
                        .orElse(MediaType.octetstream)
                    )
            ).orElse(MediaType.octetstream)
        );
  }

  @Override
  public void close() throws IOException {
    // undertow will delete the file, we don't have to worry about.
  }

  @Override
  public String name() {
    return value.getFileName();
  }

  @Override
  public MediaType type() {
    return type.get();
  }

  @Override
  public Mutant header(final String name) {
    List<String> headers = Optional
        .ofNullable(value.getHeaders())
        .<List<String>> map(
            h -> Optional.<List<String>> ofNullable(h.get(name))
                .orElse(Collections.<String> emptyList()))
        .orElse(Collections.<String> emptyList());
    return new MutantImpl(injector, name, ImmutableList.copyOf(headers), MediaType.all, charset);
  }

  @Override
  public File file() throws IOException {
    return value.getFile();
  }

}
