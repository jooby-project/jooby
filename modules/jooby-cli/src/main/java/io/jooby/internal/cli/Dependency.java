package io.jooby.internal.cli;

public class Dependency {
  private String groupId;

  private String artifactId;

  public Dependency(String groupId, String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  @Override public String toString() {
    return groupId + ":" + artifactId;
  }
}
