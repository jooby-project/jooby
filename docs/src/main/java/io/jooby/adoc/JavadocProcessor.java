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

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JavadocProcessor extends InlineMacroProcessor {

  public JavadocProcessor(String name) {
    super(name);
  }

  @Override
  public Object process(ContentNode parent, String clazz, Map<String, Object> attributes) {
    StringBuilder link = new StringBuilder("https://static.javadoc.io/io.jooby/jooby/");
    StringBuilder text = new StringBuilder();
    link.append(DocGenerator.VERSION);
    link.append("/io/jooby/").append(clazz).append(".html");

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
    } else if (variable != null) {
      link.append("#").append(variable);
      text.append(attributes.getOrDefault("text", Optional.ofNullable(arg1).orElse(clazz)));
    } else {
      text.append(attributes.getOrDefault("text", Optional.ofNullable(arg1).orElse(clazz)));
    }

    Map<String, Object> options = new HashMap<>();
    options.put("type", ":link");
    options.put("target", link.toString());
    return createPhraseNode(parent, "anchor", text.toString(), attributes, options);
  }

}
