/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.raml;

import java.util.List;
import java.util.stream.Collectors;

import org.jooby.raml.Raml;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

public class Doc {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(Raml.class);

  public static String toMarkDown(final String html) {
    Document doc = Jsoup.parseBodyFragment(html.replace("\n", "<br>"));
    StringBuilder buff = new StringBuilder();
    recurseElement(doc.body(), buff);
    return buff.toString();
  }

  public static String toYaml(final String text, final int level) {
    List<String> lines = Splitter.on("\n").splitToList(text);
    long count = lines.stream()
        .filter(l -> l.trim().length() > 0)
        .count();
    if (count == 1) {
      return "'" + text.trim().replace("'", "''") + "'";
    }
    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < level + 2; i++) {
      indent.append(" ");
    }
    return "|-\n" + lines.stream()
        .map(line -> {
          if (line.trim().length() > 0) {
            return indent + line;
          }
          return "";
        })
        .collect(Collectors.joining("\n"))
        .replaceAll("^[\\n]+", "");
  }

  // Source: https://github.com/foursquare/sites-to-markdown/blob/master/src/jon/Convert.java
  private static void recurseElement(final Element element, final StringBuilder builder) {
    new NodeTraversor(new NodeVisitor() {
      boolean isInToc = false;
      int listDepth = 0;

      @Override
      public void head(final Node node, final int depth) {
        if (!isInToc) {
          if (node instanceof TextNode) {
            TextNode textNode = (TextNode) node;
            String txt = textNode.text().replaceAll("\u00a0", " "); // non-break spaces

            builder.append(txt);
          } else if (node instanceof Element) {
            Element element = (Element) node;
            switch (element.tagName()) {
              case "span":
              case "blockquote":
                // ignored
                break;
              case "ol":
              case "ul":
                listDepth += 1;
              case "br":
              case "p":
                builder.append("\n");
                break;
              case "div":
                builder.append("\n");
                break;
              case "h1":
                builder.append("\n# ");
                break;
              case "h2":
                builder.append("\n## ");
                break;
              case "h3":
                builder.append("\n### ");
                break;
              case "h4":
                builder.append("\n#### ");
              case "b":
              case "strong":
                builder.append("**");
                break;
              case "cite":
              case "i":
              case "u":
                builder.append("*");
                break;
              case "a":
                builder.append('[');
                break;
              case "li":
                for (int i = 0; i < listDepth - 1; i++) {
                  builder.append(" ");
                }
                builder.append(element.parent().tagName().equals("ol") ? "1. " : "* ");
                break;
              case "code":
                builder.append("`");
                break;
              case "strike":
                builder.append("<").append(element.tagName()).append(">");
                break;
              case "img":
                String src = element.attr("src");
                String alt = element.attr("alt");
                alt = alt == null ? "" : alt;
                if (src != null) {
                  builder.append("![").append(alt).append("](").append(src).append(")\n");
                }

                break;
              case "pre":
                builder.append("```\n");
                break;
              case "hr":
                builder.append("\n***\n");
                break;
              case "font":
                String face = element.attr("face");
                if (face != null && face.contains("monospace")) {
                  builder.append("`");
                }
                break;
              default:
                log.debug("Unhandled element {}", element.tagName());
            }
          }
        }
      }

      @Override
      public void tail(final Node node, final int depth) {

        if (node instanceof Element) {
          Element element = (Element) node;
          switch (element.tagName()) {
            case "b":
            case "strong":
              builder.append("**");
              break;
            case "ol":
            case "ul":
              listDepth -= 1;
              break;
            case "cite":
            case "i":
            case "u":
              builder.append("*");
              break;

            case "strike":
              builder.append("</").append(element.tagName()).append(">");
              break;
            case "a":
              String href = element.attr("href");
              if (href != null) {
                if (href.startsWith("http")) {
                  builder.append(']').append('(').append(href).append(')');
                } else {
                  builder.append(']').append('(').append(href).append(')');
                }

              }
              break;
            case "pre":
              builder.append("\n```\n");
              break;
            case "code":
              builder.append("`");
              break;
            case "font":
              String face = element.attr("face");
              if (face != null && face.contains("monospace")) {
                builder.append("`");
              }
              break;
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "li":
              builder.append("\n");
            default:
              break;
          }
        }
      }
    }).traverse(element);
  }

  public static String parse(final String doc, final int level) {
    return toYaml(toMarkDown(doc), level).trim();
  }

}
