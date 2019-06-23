/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate;

import org.hibernate.boot.archive.scan.spi.ScanEnvironment;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class ScanEnvImpl implements ScanEnvironment {

  private final List<URL> packages;

  public ScanEnvImpl(final List<URL> packages)  {
    this.packages = packages;
  }

  @Override
  public URL getRootUrl() {
    return null;
  }

  @Override
  public List<URL> getNonRootUrls() {
    return packages;
  }

  @Override
  public List<String> getExplicitlyListedClassNames() {
    return Collections.emptyList();
  }

  @Override
  public List<String> getExplicitlyListedMappingFiles() {
    return Collections.emptyList();
  }

}
