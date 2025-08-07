/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocNode.getText;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.children;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import io.jooby.SneakyThrows.Consumer2;
import io.jooby.SneakyThrows.Consumer3;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.ResponseExt;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class JavaDocTag {
  private static final Predicate<DetailNode> CUSTOM_TAG =
      javadocToken(JavadocTokenTypes.CUSTOM_NAME);
  private static final Predicate<DetailNode> TAG =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@tag.") || it.getText().equals("@tag"));
  private static final Predicate<DetailNode> SERVER =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@server."));
  private static final Predicate<DetailNode> CONTACT =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@contact."));
  private static final Predicate<DetailNode> LICENSE =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@license."));
  private static final Predicate<DetailNode> OPERATION_ID =
      CUSTOM_TAG.and(it -> it.getText().equals("@operationId"));
  private static final Predicate<DetailNode> EXTENSION =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@x-"));
  private static final Predicate<DetailNode> THROWS =
      it -> tree(it).anyMatch(javadocToken(JavadocTokenTypes.THROWS_LITERAL));

  public static List<Server> servers(DetailNode node) {
    return openApiComponent(
        node,
        SERVER,
        "server",
        hash -> {
          var server = new Server();
          server.setDescription((String) hash.get("description"));
          server.setUrl((String) hash.get("url"));
          return server;
        });
  }

  public static List<Contact> contacts(DetailNode node) {
    return openApiComponent(
        node,
        CONTACT,
        "contact",
        hash -> {
          var item = new Contact();
          item.setName((String) hash.get("name"));
          item.setUrl((String) hash.get("url"));
          item.setEmail((String) hash.get("email"));
          return item;
        });
  }

  public static List<License> license(DetailNode node) {
    return openApiComponent(
        node,
        LICENSE,
        "license",
        hash -> {
          var item = new License();
          item.setName((String) hash.get("name"));
          item.setUrl((String) hash.get("url"));
          return item;
        });
  }

  private static <T> List<T> openApiComponent(
      DetailNode node, Predicate<DetailNode> filter, String path, Function<Map<?, ?>, T> mapper) {
    var values = new ArrayList<String>();
    javaDocTag(
        node,
        filter,
        (tag, value) -> {
          values.add(tag.getText().substring(1));
          values.add(value);
        });
    var result = new ArrayList<T>();
    if (!values.isEmpty()) {
      var output = TinyYamlDocParser.parse(values);
      var itemList = output.get(path);
      if (!(itemList instanceof List<?>)) {
        itemList = List.of(itemList);
      }
      //noinspection unchecked,rawtypes
      ((List) itemList)
          .forEach(
              it -> {
                if (it instanceof Map<?, ?> hash) {
                  result.add(mapper.apply(hash));
                }
              });
    }
    return result;
  }

  public static Map<StatusCode, ResponseExt> throwList(DetailNode node) {
    var result = new LinkedHashMap<StatusCode, ResponseExt>();
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
            if (text == null) {
              text = statusCode.reason();
            } else {
              text = statusCode.reason() + ": " + text;
            }
            var throwsDoc = new ResponseExt(Integer.toString(statusCode.value()));
            throwsDoc.setDescription(text);
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
    return TinyYamlDocParser.parse(values);
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
      var tagMap = TinyYamlDocParser.parse(values);
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

  public static String operationId(DetailNode javadoc) {
    var operationId = new ArrayList<String>();
    javaDocTag(
        javadoc,
        OPERATION_ID,
        (tag, value, text) -> {
          operationId.add(text);
        });
    return operationId.isEmpty() ? null : operationId.getFirst();
  }
}
