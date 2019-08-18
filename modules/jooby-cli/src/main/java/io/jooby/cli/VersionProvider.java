/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.cli;

import picocli.CommandLine;

import java.util.Objects;
import java.util.Optional;

public class VersionProvider implements CommandLine.IVersionProvider {

  @Override public String[] getVersion() throws Exception {
    String version = Optional.ofNullable(getClass().getPackage())
        .map(Package::getImplementationVersion)
        .filter(Objects::nonNull)
        .orElse("0.0.0");
    return new String[] {version};
  }
}
