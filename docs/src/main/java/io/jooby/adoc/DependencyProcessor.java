/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Reader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class DependencyProcessor extends BlockProcessor {

  private final Document pom;

  public DependencyProcessor(String name, Map<String, Object> config) throws IOException {
    super(name, config);
    pom = Jsoup
        .parse(DocGenerator.basedir().getParent().resolve("modules").resolve("jooby-bom").resolve("pom.xml").toFile(), "UTF-8");
  }

  @Override
  public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
    List<String> lines = new ArrayList<>();
    String[] artifactId = ((String) attributes.get("artifactId")).split("\\s*,\\s*");

    maven((String) attributes.get("groupId"), artifactId, (String) attributes.get("version"),
        lines::add);

    lines.add("");

    gradle((String) attributes.get("groupId"), artifactId, (String) attributes.get("version"),
        lines::add);
    lines.add("");

    parseContent(parent, lines);
    return null;
  }

  private String groupId(String artifactId) {
    if (artifactId.startsWith("jooby-")) {
      return "io.jooby";
    }
    return findArtifact(artifactId)
        .select("groupId").text().trim();
  }

  private String version(String artifactId) {
    if (artifactId.startsWith("jooby-")) {
      return pom.selectFirst("version").text().trim();
    }
    String version = findArtifact(artifactId)
        .select("version").text().trim();
    if (version.startsWith("${") && version.endsWith("}")) {
      String versionProp = version.substring(2, version.length() - 1);
      version = pom.select("properties > *").stream()
          .filter(it -> versionProp.equalsIgnoreCase(it.tagName()))
          .findFirst()
          .map(Element::text)
          .orElseThrow(() -> new IllegalArgumentException("Missing version: " + artifactId));
    }
    return version;
  }

  private Element findArtifact(String artifactId) {
    return dependencies().stream()
        .filter(it -> it.select("artifactId").text().equalsIgnoreCase(artifactId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Missing artifact: " + artifactId));
  }

  private void gradle(String groupId, String[] artifactId, String version, Consumer<String> lines) {
    lines.accept(".Gradle");
    lines.accept("[source,javascript,role=\"secondary\"]");
    lines.accept("----");
    for (int i = 0; i < artifactId.length; i++) {
      if (i > 0) {
        lines.accept("");
      }
      comment(artifactId[i], "//", "").ifPresent(lines::accept);
      lines.accept(
          "implementation '" + (groupId == null ? groupId(artifactId(artifactId[i])) : groupId) + ":" + artifactId(artifactId[i])
              + ":" + (version == null ? version(artifactId(artifactId[i])) : version)
              + "'");
    }
    lines.accept("----");
  }

  private String artifactId(String artifactId) {
    int s = artifactId.indexOf(":");
    return s > 0 ? artifactId.substring(0, s): artifactId;
  }

  private Optional<String> comment(String text, String prefix, String suffix) {
    int s = text.indexOf(":");
    return s > 0 ? Optional.of(prefix + " " + text.substring(s + 1) + suffix) : Optional.empty();
  }

  private void maven(String groupId, String[] artifactId, String version, Consumer<String> lines) {
    lines.accept(".Maven");
    lines.accept("[source, xml,role=\"primary\"]");
    lines.accept("----");
    for (int i = 0; i < artifactId.length; i++) {
      if (i > 0) {
        lines.accept("");
      }
      comment(artifactId[i], "<!--", "-->").ifPresent(lines::accept);
      lines.accept("<dependency>");
      lines.accept(
          "  <groupId>" + (groupId == null ? groupId(artifactId(artifactId[i])) : groupId) + "</groupId>");
      lines.accept("  <artifactId>" + artifactId(artifactId[i]) + "</artifactId>");
      lines.accept(
          "  <version>" + (version == null ? version(artifactId(artifactId[i])) : version) + "</version>");
      lines.accept("</dependency>");
    }
    lines.accept("----");
  }

  private Elements dependencies() {
    return pom.select("dependencyManagement")
        .select("dependencies")
        .select("dependency");
  }
}
