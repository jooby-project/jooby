/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DependencyProcessor extends BlockProcessor {

  public DependencyProcessor(String name, Map<String, Object> config) {
    super(name, config);
  }

  @Override
  public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
    List<String> lines = new ArrayList<>();
    maven(
        attributes.getOrDefault("groupId", "io.jooby"),
        attributes.get("artifactId"),
        attributes.getOrDefault("version", JoobyDoc.VERSION),
        lines::add
    );
    lines.add("");

    gradle(
        attributes.getOrDefault("groupId", "io.jooby"),
        attributes.get("artifactId"),
        attributes.getOrDefault("version", JoobyDoc.VERSION),
        lines::add
    );
    lines.add("");

    parseContent(parent, lines);
    return null;
  }

  private void gradle(Object groupId, Object artifactId, Object version, Consumer<String> lines) {
    lines.accept(".Gradle");
    lines.accept("[source,javascript,role=\"secondary\"]");
    lines.accept("----");
    lines.accept("compile: '" + groupId + ":" + artifactId + ":" + version + "'");
    lines.accept("----");
  }

  private void maven(Object groupId, Object artifactId, Object version, Consumer<String> lines) {
    lines.accept(".Maven");
    lines.accept("[source, xml,role=\"primary\"]");
    lines.accept("----");
    lines.accept("<dependency>");
    lines.accept("  <groupId>" + groupId + "</groupId>");
    lines.accept("  <artifactId>" + artifactId + "</artifactId>");
    lines.accept("  <version>" + version + "</version>");
    lines.accept("</dependency>");
    lines.accept("----");
  }
}
