/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import static io.jooby.SneakyThrows.throwingFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import io.jooby.SneakyThrows;

public class Dependencies {

  public static class Dependency {
    public String groupId;

    public String artifactId;

    public String version;

    @Override
    public String toString() {
      return groupId + ":" + artifactId + ":" + version;
    }
  }

  private Map<String, Dependency> dependencyMap = new HashMap<>();

  private static final Dependencies instance = new Dependencies();

  private Dependencies() {
    try {
      for (Document pom : pomList()) {
        collectDependencies(pom, pom.select("dependencyManagement").select("dependencies"));
        collectDependencies(pom, pom.select("dependencies"));
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private void collectDependencies(Document pom, Elements dependencies) {
    for (Element dependency : dependencies.select("dependency")) {
      Dependency dep = new Dependency();
      dep.groupId = dependency.select("groupId").text();
      dep.artifactId = dependency.select("artifactId").text();
      dep.version = findVersion(pom, dep.artifactId, dependency.select("version").text());

      dependencyMap.put(dep.artifactId, dep);
    }
  }

  public static Dependencies.Dependency get(String artifactId) {
    Dependency dep = instance.dependencyMap.get(artifactId);
    if (dep == null) {
      throw new IllegalArgumentException("Missing artifact: " + artifactId);
    }
    return dep;
  }

  private List<Document> pomList() throws IOException {
    List<Document> poms = new ArrayList<>();
    Document jooby =
        Jsoup.parse(DocGenerator.basedir().getParent().resolve("pom.xml").toFile(), "UTF-8");
    poms.add(jooby);
    try (Stream<Path> tree = Files.walk(DocGenerator.basedir().getParent())) {
      tree.filter(Files::isRegularFile)
          .filter(it -> it.toString().endsWith("pom.xml"))
          .map(throwingFunction(it -> Jsoup.parse(it.toFile())))
          .forEach(poms::add);
    }
    jooby.select("dependencyManagement").select("dependencies").select("dependency").stream()
        .filter(it -> it.select("type").text().equals("pom"))
        .map(
            it -> {
              String artifactId = it.select("artifactId").text();
              String versionRef = it.select("version").text();
              String version = findVersion(jooby, artifactId, versionRef);
              Path location = Paths.get(System.getProperty("user.home"), ".m2", "repository");
              location =
                  Stream.of(it.select("groupId").text().split("\\."))
                      .reduce(location, Path::resolve, Path::resolve);
              location = location.resolve(artifactId);
              location = location.resolve(version);
              location = location.resolve(artifactId + "-" + version + ".pom");
              return location;
            })
        .map(throwingFunction(it -> Jsoup.parse(it.toFile())))
        .forEach(poms::add);
    return poms;
  }

  private static String findVersion(Document pom, String artifactId, String versionRef) {
    if (versionRef.equals("${project.version}")) {
      return pom.select("version").first().text();
    }
    if (versionRef.startsWith("${")) {
      return pom.select("properties > *").stream()
          .filter(e -> versionRef.equalsIgnoreCase("${" + e.tagName() + "}"))
          .findFirst()
          .map(Element::text)
          .map(
              it -> {
                if (it.equals("${project.version}")) {
                  return pom.select("version").first().text();
                }
                return it;
              })
          .orElseGet(() -> pom.select("version").first().text());
    }
    return versionRef;
  }
}
