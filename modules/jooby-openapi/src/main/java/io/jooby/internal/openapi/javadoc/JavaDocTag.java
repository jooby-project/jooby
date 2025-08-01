/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocNode.getText;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.*;
import static io.jooby.internal.openapi.javadoc.JavaDocSupport.children;

import java.util.*;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import io.jooby.SneakyThrows.Consumer2;
import io.jooby.SneakyThrows.Consumer3;
import io.jooby.StatusCode;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class JavaDocTag {
  private static final Predicate<DetailNode> CUSTOM_TAG =
      javadocToken(JavadocTokenTypes.CUSTOM_NAME);
  private static final Predicate<DetailNode> TAG =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@tag.") || it.getText().equals("@tag"));
  private static final Predicate<DetailNode> SERVER =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@server."));
  private static final Predicate<DetailNode> EXTENSION =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@x-"));
  private static final Predicate<DetailNode> THROWS =
      it -> tree(it).anyMatch(javadocToken(JavadocTokenTypes.THROWS_LITERAL));

  @SuppressWarnings("unchecked")
  public static List<Server> servers(DetailNode node) {
    var values = new ArrayList<String>();
    javaDocTag(
        node,
        SERVER,
        (tag, value) -> {
          values.add(tag.getText().substring(1));
          values.add(value);
        });
    var result = new ArrayList<Server>();
    if (!values.isEmpty()) {
      var serverMap = MiniYamlDocParser.parse(values);
      var servers = serverMap.get("server");
      if (!(servers instanceof List<?>)) {
        servers = List.of(servers);
      }
      ((List) servers)
          .forEach(
              it -> {
                if (it instanceof Map<?, ?> hash) {
                  var server = new Server();
                  server.setDescription((String) hash.get("description"));
                  server.setUrl((String) hash.get("url"));
                  result.add(server);
                }
              });
    }
    return result;
  }

  public static Map<StatusCode, ThrowsDoc> throwList(DetailNode node) {
    var result = new LinkedHashMap<StatusCode, ThrowsDoc>();
    javaDocTag(
        node,
        THROWS,
        (tag, text) -> {
          var statusCode =
              tree(tag)
                  .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                  .findFirst()
                  .flatMap(
                      it ->
                          tree(it)
                              .filter(javadocToken(JavadocTokenTypes.HTML_TAG_NAME))
                              .filter(tagName -> tagName.getText().equals("code"))
                              .flatMap(
                                  tagName ->
                                      backward(tagName)
                                          .filter(javadocToken(JavadocTokenTypes.HTML_TAG))
                                          .findFirst()
                                          .stream())
                              .flatMap(
                                  htmlTag ->
                                      children(htmlTag)
                                          .filter(javadocToken(JavadocTokenTypes.TEXT))
                                          .findFirst()
                                          .stream())
                              .map(DetailNode::getText)
                              .map(
                                  value -> {
                                    try {
                                      return Integer.parseInt(value);
                                    } catch (NumberFormatException e) {
                                      return null;
                                    }
                                  })
                              .filter(Objects::nonNull)
                              .filter(code -> code >= 400 && code <= 600)
                              .map(StatusCode::valueOf)
                              .findFirst())
                  .orElse(null);
          if (statusCode != null) {
            var throwsDoc = new ThrowsDoc(statusCode, text);
            result.putIfAbsent(statusCode, throwsDoc);
          }
        });
    return result;
  }

  public static Map<String, Object> extensions(DetailNode node) {
    var values = new ArrayList<String>();
    javaDocTag(
        node,
        EXTENSION,
        (tag, value) -> {
          // Strip '@'
          values.add(tag.getText().substring(1));
          values.add(value);
        });
    return MiniYamlDocParser.parse(values);
  }

  public static List<Tag> tags(DetailNode node) {
    var result = new ArrayList<Tag>();
    var values = new ArrayList<String>();
    javaDocTag(
        node,
        TAG,
        (tag, value) -> {
          if (tag.getText().equals("@tag")) {
            // Process single line tag:
            // - @tag Book. Book Operations
            // - @tag Book
            var dot = value.indexOf(".");
            var tagName = value;
            String tagDescription = null;
            if (dot > 0) {
              tagName = value.substring(0, dot);
              if (dot + 1 < value.length()) {
                tagDescription = value.substring(dot + 1).trim();
                if (tagDescription.isBlank()) {
                  tagDescription = null;
                }
              }
            }
            if (!tagName.trim().isEmpty()) {

              result.add(createTag(tagName, tagDescription));
            }
          } else {
            values.add(tag.getText().substring(1));
            values.add(value);
          }
        });
    if (!values.isEmpty()) {
      var tagMap = MiniYamlDocParser.parse(values);
      var tags = tagMap.get("tag");
      if (!(tags instanceof List<?>)) {
        tags = List.of(tags);
      }
      ((List) tags)
          .forEach(
              e -> {
                if (e instanceof Map<?, ?> hash) {
                  result.add(
                      createTag((String) hash.get("name"), (String) hash.get("description")));
                }
              });
    }
    return result;
  }

  private static Tag createTag(String tagName, String tagDescription) {
    Tag tag = new Tag();
    tag.setName(tagName);
    tag.setDescription(tagDescription);
    return tag;
  }

  public static void javaDocTag(
      DetailNode tree, Predicate<DetailNode> filter, Consumer2<DetailNode, String> consumer) {
    javaDocTag(tree, filter, (tag, value, text) -> consumer.accept(tag, text));
  }

  public static void javaDocTag(
      DetailNode tree,
      Predicate<DetailNode> filter,
      Consumer3<DetailNode, DetailNode, String> consumer) {
    if (tree != JavaDocNode.EMPTY_NODE) {
      for (var tag : tree(tree).filter(javadocToken(JavadocTokenTypes.JAVADOC_TAG)).toList()) {
        var tagName = tree(tag).filter(filter).findFirst().orElse(null);
        if (tagName != null) {
          var tagValue =
              tree(tag)
                  .filter(javadocToken(JavadocTokenTypes.DESCRIPTION))
                  .findFirst()
                  .orElse(null);
          var tagText = tagValue == null ? null : getText(List.of(tagValue.getChildren()), true);
          consumer.accept(tagName, tagValue, tagText);
        }
      }
    }
  }
}
