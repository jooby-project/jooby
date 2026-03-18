/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.javadoc;

import static io.jooby.javadoc.JavaDocNode.getText;
import static io.jooby.javadoc.JavaDocStream.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocCommentsTokenTypes;

class JavaDocTag {
  private static final Predicate<DetailNode> TAG_SHORT = it -> it.getText().equals("tag");
  private static final Predicate<DetailNode> TAG = it -> it.getText().startsWith("tag.");
  private static final Predicate<DetailNode> SERVER = it -> it.getText().startsWith("server.");
  private static final Predicate<DetailNode> SECURITY =
      it -> it.getText().equals("security") || it.getText().equals("securityRequirement");
  private static final Predicate<DetailNode> SECURITY_SCHEME =
      it -> it.getText().startsWith("securityScheme.");
  private static final Predicate<DetailNode> CONTACT = it -> it.getText().startsWith("contact.");
  private static final Predicate<DetailNode> LICENSE = it -> it.getText().startsWith("license.");
  private static final Predicate<DetailNode> OPERATION_ID =
      it -> it.getText().equals("operationId");
  private static final Predicate<DetailNode> EXTENSION = it -> it.getText().startsWith("x-");
  private static final Predicate<DetailNode> THROWS =
      it -> tree(it).anyMatch(javadocToken(JavadocCommentsTokenTypes.THROWS_BLOCK_TAG));
  private static final Predicate<DetailNode> PARAM =
      it -> tree(it).anyMatch(javadocToken(JavadocCommentsTokenTypes.PARAM_BLOCK_TAG));

  public static Map<String, String> getParametersDoc(DetailNode node) {
    var parameters = new LinkedHashMap<String, String>();
    javaDocTag(
        node,
        PARAM,
        (tag, value) -> {
          tree(tag)
              .filter(javadocToken(JavadocCommentsTokenTypes.PARAMETER_NAME))
              .findFirst()
              .map(DetailNode::getText)
              .ifPresent(
                  name ->
                      tree(tag)
                          .filter(javadocToken(JavadocCommentsTokenTypes.DESCRIPTION))
                          .findFirst()
                          .map(description -> getText(tree(description).toList(), true))
                          .ifPresent(text -> parameters.put(name, text)));
        });
    return parameters;
  }

  public static String getReturnDoc(DetailNode node) {
    var text = new StringBuilder();
    javaDocTag(
        node,
        javadocToken(JavadocCommentsTokenTypes.RETURN_BLOCK_TAG),
        (tag, value) -> {
          tree(tag.getParent())
              .filter(javadocToken(JavadocCommentsTokenTypes.DESCRIPTION))
              .findFirst()
              .map(description -> getText(tree(description).toList(), true))
              .ifPresent(text::append);
        });
    var result = text.toString().trim();
    return result.isEmpty() ? null : result;
  }

  public static List<Map<String, List<String>>> securityRequirement(DetailNode node) {
    return parse(node, SECURITY, null).stream()
        .map(
            hash ->
                hash.containsKey("security")
                    ? hash.get("security")
                    : hash.get("securityRequirement"))
        .map(Object::toString)
        .map(String::trim)
        .map(
            value -> {
              // look first space
              var indexOf = value.indexOf(' ');
              String key;
              String scopes;
              if (indexOf > 0) {
                key = value.substring(0, indexOf).trim();
                scopes = value.substring(indexOf + 1).trim();
              } else {
                key = value;
                scopes = "";
              }
              if (scopes.startsWith("[") && scopes.endsWith("]")) {
                scopes = scopes.substring(1, scopes.length() - 1);
              }

              var scopeList =
                  Stream.of(scopes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

              return Map.of(key, scopeList);
            })
        .toList();
  }

  public static Map<String, Map<String, Object>> securitySchemes(DetailNode node) {
    var result = new LinkedHashMap<String, Map<String, Object>>();
    parse(node, SECURITY_SCHEME, "securityScheme")
        .forEach(
            hash -> {
              // Extract the identifier (e.g., "myOauth2Security")
              var name = (String) hash.get("name");
              if (name != null) {
                result.put(name, (Map<String, Object>) hash);
              }
            });
    return result;
  }

  public static List<Map<String, Object>> servers(DetailNode node) {
    return parse(node, SERVER, "server");
  }

  public static List<Map<String, Object>> contacts(DetailNode node) {
    return parse(node, CONTACT, "contact");
  }

  public static List<Map<String, Object>> license(DetailNode node) {
    return parse(node, LICENSE, "license");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> parse(
      DetailNode node, Predicate<DetailNode> filter, String path) {
    var values = new ArrayList<String>();
    javaDocTag(
        node,
        filter,
        (tag, value) -> {
          values.add(tag.getText());
          values.add(value);
        });
    return JavaDocObjectParser.parse(values).stream()
        .map(hash -> path == null ? hash : (Map<String, Object>) hash.get(path))
        .toList();
  }

  public static Map<Integer, String> throwList(DetailNode node) {
    var result = new LinkedHashMap<Integer, String>();
    javaDocTag(
        node,
        THROWS,
        (tag, text) -> {
          // Search the entire description subtree for a TEXT node containing exactly a 3-digit
          // number
          var statusCode =
              tree(tag)
                  .filter(javadocToken(JavadocCommentsTokenTypes.DESCRIPTION))
                  .flatMap(JavaDocStream::tree) // Flatten all nested elements (including HTML tags)
                  .filter(javadocToken(JavadocCommentsTokenTypes.TEXT))
                  .map(DetailNode::getText)
                  .map(String::trim)
                  .filter(s -> s.matches("\\d{3}")) // Only match isolated status codes like "400"
                  .map(Integer::parseInt)
                  .filter(code -> code >= 400 && code <= 600)
                  .findFirst()
                  .orElse(null);

          if (statusCode != null) {
            String finalText = reason(statusCode);
            if (text != null && !text.trim().isEmpty()) {
              finalText += ": " + text;
            }
            result.putIfAbsent(statusCode, finalText);
          }
        });
    return result;
  }

  public static Map<String, Object> extensions(DetailNode node) {
    return parse(node, EXTENSION, null).stream().findFirst().orElse(Map.of());
  }

  public static List<DocTag> tags(DetailNode node) {
    var tags =
        parse(node, TAG_SHORT, null).stream()
            .map(hash -> hash.get("tag").toString().trim())
            .map(
                value -> {
                  var indexOf = value.indexOf('.');
                  String name;
                  String description;
                  if (indexOf > 0) {
                    name = value.substring(0, indexOf).trim();
                    description = value.substring(indexOf + 1).trim();
                  } else {
                    name = value;
                    description = null;
                  }
                  return new DocTag(name, description);
                })
            .collect(Collectors.toList());

    parse(node, TAG, "tag").stream()
        .map(hash -> new DocTag((String) hash.get("name"), (String) hash.get("description")))
        .forEach(tags::add);
    return tags;
  }

  public static void javaDocTag(
      DetailNode tree, Predicate<DetailNode> filter, Utils.Consumer2<DetailNode, String> consumer) {
    javaDocTag(tree, filter, (tag, value, text) -> consumer.accept(tag, text));
  }

  public static void javaDocTag(
      DetailNode tree,
      Predicate<DetailNode> filter,
      Utils.Consumer3<DetailNode, DetailNode, String> consumer) {
    if (tree != JavaDocNode.EMPTY_NODE) {
      for (var tag :
          tree(tree).filter(javadocToken(JavadocCommentsTokenTypes.JAVADOC_BLOCK_TAG)).toList()) {
        var tagName = tree(tag).filter(filter).findFirst().orElse(null);
        if (tagName != null) {
          var tagValue =
              tree(tag)
                  .filter(javadocToken(JavadocCommentsTokenTypes.DESCRIPTION))
                  .findFirst()
                  .orElse(null);
          var tagText = tagValue == null ? null : getText(children(tagValue).toList(), true);
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

  /**
   * Return the reason phrase of the given HTTP status code.
   *
   * @param statusCode the numeric value of the status code
   * @return the reason phrase of the status code
   */
  public static String reason(int statusCode) {
    return switch (statusCode) {
      case 100 -> "Continue";
      case 101 -> "Switching Protocols";
      case 102 -> "Processing";
      case 103 -> "Checkpoint";
      case 200 -> "Success";
      case 201 -> "Created";
      case 202 -> "Accepted";
      case 203 -> "Non-Authoritative Information";
      case 204 -> "No Content";
      case 205 -> "Reset Content";
      case 206 -> "Partial Content";
      case 207 -> "Multi-StatusCode";
      case 208 -> "Already Reported";
      case 226 -> "IM Used";
      case 300 -> "Multiple Choices";
      case 301 -> "Moved Permanently";
      case 302 -> "Found";
      case 303 -> "See Other";
      case 304 -> "Not Modified";
      case 305 -> "Use Proxy";
      case 307 -> "Temporary Redirect";
      case 308 -> "Resume Incomplete";
      case 400 -> "Bad Request";
      case 401 -> "Unauthorized";
      case 402 -> "Payment Required";
      case 403 -> "Forbidden";
      case 404 -> "Not Found";
      case 405 -> "Method Not Allowed";
      case 406 -> "Not Acceptable";
      case 407 -> "Proxy Authentication Required";
      case 408 -> "Request Timeout";
      case 409 -> "Conflict";
      case 410 -> "Gone";
      case 411 -> "Length Required";
      case 412 -> "Precondition Failed";
      case 413 -> "Request Entity Too Large";
      case 414 -> "Request-URI Too Long";
      case 415 -> "Unsupported Media Type";
      case 416 -> "Requested range not satisfiable";
      case 417 -> "Expectation Failed";
      case 418 -> "I'm a teapot";
      case 422 -> "Unprocessable Entity";
      case 423 -> "Locked";
      case 424 -> "Failed Dependency";
      case 426 -> "Upgrade Required";
      case 428 -> "Precondition Required";
      case 429 -> "Too Many Requests";
      case 431 -> "Request Header Fields Too Large";
      case 499 -> "The client aborted the request before completion";
      case 500 -> "Server Error";
      case 501 -> "Not Implemented";
      case 502 -> "Bad Gateway";
      case 503 -> "Service Unavailable";
      case 504 -> "Gateway Timeout";
      case 505 -> "HTTP Version not supported";
      case 506 -> "Variant Also Negotiates";
      case 507 -> "Insufficient Storage";
      case 508 -> "Loop Detected";
      case 509 -> "Bandwidth Limit Exceeded";
      case 510 -> "Not Extended";
      case 511 -> "Network Authentication Required";
      default -> Integer.toString(statusCode);
    };
  }
}
