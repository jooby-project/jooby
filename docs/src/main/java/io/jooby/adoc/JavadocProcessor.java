/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.adoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class JavadocProcessor extends InlineMacroProcessor {

  public JavadocProcessor(String name) {
    super(name);
  }

  @Override
  public PhraseNode process(StructuralNode parent, String clazz, Map<String, Object> attributes) {
    StringBuilder link = generateLink(attributes);

    StringBuilder text = new StringBuilder();
    String[] names = clazz.split("\\.");
    List<String> pkg = new ArrayList<>();
    List<String> nameList = new ArrayList<>();
    for (String name : names) {
      if (Character.isUpperCase(name.charAt(0))) {
        nameList.add(name);
      } else {
        pkg.add(name);
      }
    }
    if (!pkg.isEmpty()) {
      link.append(String.join("/", pkg)).append("/");
    }
    String classname = String.join(".", nameList);
    link.append(classname).append(".html");

    String arg1 = (String) attributes.get("1");
    String method = null;
    String variable = null;
    if (arg1 != null) {
      if (Character.isLowerCase(arg1.charAt(0))) {
        method = arg1;
      }
      if (arg1.chars().allMatch(c -> Character.isUpperCase(c) || c == '_')) {
        // ENUM or constant
        variable = arg1;
      }
    }
    if (method != null) {
      link.append("#").append(method).append("(");
      text.append(method).append("(");
      int index = 2;
      while (attributes.get(String.valueOf(index)) != null) {
        String qualifiedType = attributes.get(String.valueOf(index)).toString();
        link.append(qualifiedType.replace("[]", ":A").replace("&#8230;&#8203;", "..."));

        int start = qualifiedType.lastIndexOf('.');
        String simpleName = start > 0 ? qualifiedType.substring(start + 1) : qualifiedType;

        text.append(simpleName);

        index += 1;

        if (attributes.get(String.valueOf(index)) != null) {
          link.append(",");
          text.append(",");
        }
      }
      link.append(")");
      String label = (String) attributes.get("text");
      if (label != null) {
        text.setLength(0);
        text.append(label);
      } else {
        text.append(")");
      }
    } else if (variable != null) {
      link.append("#").append(variable);
      text.append(attributes.getOrDefault("text", arg1));
    } else {
      text.append(attributes.getOrDefault("text", arg1 != null ? arg1 : classname));
    }

    Map<String, Object> options = new HashMap<>();
    options.put("type", ":link");
    options.put("target", link.toString());
    return createPhraseNode(parent, "anchor", text.toString(), attributes, options);
  }

  private static StringBuilder generateLink(Map<String, Object> attributes) {

    String artifact = (String) attributes.getOrDefault("artifact", "jooby");
    return new StringBuilder("https://www.javadoc.io/doc/io.jooby/")
        .append(artifact)
        .append("/latest/io.")
        .append(artifact.replace('-', '.'))
        .append("/io/jooby/");
  }
}
