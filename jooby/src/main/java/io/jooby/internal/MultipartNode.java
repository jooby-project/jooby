/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.Multipart;
import io.jooby.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultipartNode extends HashValue implements Multipart {
  private Map<String, List<FileUpload>> files = new HashMap<>();

  public MultipartNode(Context ctx, String name) {
    super(ctx, name);
  }

  @Override public void put(String name, FileUpload file) {
    files.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
  }

  @Nonnull @Override public List<FileUpload> files() {
    return files.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  @Nonnull @Override public List<FileUpload> files(@Nonnull String name) {
    return this.files.getOrDefault(name, Collections.emptyList());
  }

  @Nonnull @Override public FileUpload file(@Nonnull String name) {
    List<FileUpload> files = files(name);
    if (files.isEmpty()) {
      throw SneakyThrows.propagate(new FileNotFoundException(name));
    }
    return files.get(0);
  }
}
