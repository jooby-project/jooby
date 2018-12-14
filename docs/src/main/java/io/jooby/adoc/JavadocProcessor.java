package io.jooby.adoc;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavadocProcessor extends InlineMacroProcessor {

  public JavadocProcessor(String name) {
    super(name);
  }

  @Override
  public Object process(ContentNode parent, String clazz, Map<String, Object> attributes) {
    StringBuilder link = new StringBuilder("https://static.javadoc.io/io.jooby/jooby/");
    StringBuilder text = new StringBuilder();
    link.append(JoobyDoc.VERSION);
    link.append("/io/jooby/").append(clazz).append(".html");

    String arg1 = (String) attributes.get("1");
    String method = null;
    if (arg1 != null) {
      if (Character.isLowerCase(arg1.charAt(0))) {
        method = arg1;
      }
    }
    if (method != null) {
      link.append("#").append(method).append("-");
      text.append(method).append("(");
      int index = 2;
      while (attributes.get(String.valueOf(index)) != null) {
        String qualifiedType = attributes.get(String.valueOf(index)).toString();
        link.append(qualifiedType);

        int start = qualifiedType.lastIndexOf('.');
        String simpleName = start > 0 ? qualifiedType.substring(start + 1) : qualifiedType;

        text.append(simpleName);

        index += 1;

        if (attributes.get(String.valueOf(index)) != null) {
          link.append(",");
          text.append(",");
        }
      }
      link.append("-");
      text.append(")");
    } else {
      text.append(attributes.getOrDefault("text", Optional.ofNullable(arg1).orElse(clazz)));
    }

    Map<String, Object> options = new HashMap<>();
    options.put("type", ":link");
    options.put("target", link.toString());
    return createPhraseNode(parent, "anchor", text.toString(), attributes, options);
  }

}
