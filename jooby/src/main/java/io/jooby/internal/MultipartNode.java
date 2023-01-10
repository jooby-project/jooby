/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.*;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.SneakyThrows;

public class MultipartNode extends HashValue implements Formdata {
  private Map<String, List<FileUpload>> files = new HashMap<>();

  public MultipartNode(Context ctx) {
    super(ctx);
  }

  @Override
  public void put(String name, FileUpload file) {
    files.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
  }

  @NonNull @Override
  public List<FileUpload> files() {
    return files.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  @NonNull @Override
  public List<FileUpload> files(@NonNull String name) {
    return this.files.getOrDefault(name, Collections.emptyList());
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
