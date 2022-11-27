/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.cli;

public class Dependency {
  private String groupId;

  private String artifactId;

  private String version;

  public Dependency(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return version == null
        ? groupId + ":" + artifactId
        : groupId + ":" + artifactId + ":" + version;
  }
}
