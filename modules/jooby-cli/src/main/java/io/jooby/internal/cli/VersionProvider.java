/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.cli;

import picocli.CommandLine;

import java.util.Objects;
import java.util.Optional;

public class VersionProvider implements CommandLine.IVersionProvider {

  @Override public String[] getVersion() {
    return new String[] {version()};
  }

  public static String version() {
    return Optional.ofNullable(VersionProvider.class.getPackage())
        .map(Package::getImplementationVersion)
        .filter(Objects::nonNull)
        .orElse("0.0.0");
  }
}
