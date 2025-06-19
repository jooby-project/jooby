/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.SneakyThrows;
import io.jooby.value.ValueFactory;

public class MultipartNode extends HashValue implements Formdata {
  private final Map<String, List<FileUpload>> files = new HashMap<>();

  public MultipartNode(ValueFactory valueFactory) {
    super(valueFactory);
  }

  @Override
  public void put(@NonNull String name, @NonNull FileUpload file) {
    files.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
  }

  @NonNull @Override
  public List<FileUpload> files() {
    return files.values().stream().flatMap(Collection::stream).toList();
  }

  @NonNull @Override
  public List<FileUpload> files(@NonNull String name) {
    return this.files.getOrDefault(name, List.of());
  }

  @NonNull @Override
  public FileUpload file(@NonNull String name) {
    List<FileUpload> files = files(name);
    if (files.isEmpty()) {
      final String error = "Field '" + name + "' is missing";
      throw SneakyThrows.propagate(new NoSuchElementException(error));
    }
    return files.get(0);
  }
}
