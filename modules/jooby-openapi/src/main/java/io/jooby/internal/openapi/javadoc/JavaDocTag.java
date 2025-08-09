/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static io.jooby.internal.openapi.javadoc.JavaDocNode.getText;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.*;
import static io.jooby.internal.openapi.javadoc.JavaDocStream.children;
import static java.util.Optional.ofNullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.puppycrawl.tools.checkstyle.api.DetailNode;
import com.puppycrawl.tools.checkstyle.api.JavadocTokenTypes;
import io.jooby.SneakyThrows.Consumer2;
import io.jooby.SneakyThrows.Consumer3;
import io.jooby.StatusCode;
import io.jooby.internal.openapi.ResponseExt;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public class JavaDocTag {
  private static final Predicate<DetailNode> CUSTOM_TAG =
      javadocToken(JavadocTokenTypes.CUSTOM_NAME);
  private static final Predicate<DetailNode> TAG_SHORT =
      CUSTOM_TAG.and(it -> it.getText().equals("@tag"));
  private static final Predicate<DetailNode> TAG =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@tag."));
  private static final Predicate<DetailNode> SERVER =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@server."));
  private static final Predicate<DetailNode> SECURITY =
      CUSTOM_TAG.and(
          it -> it.getText().equals("@security") || it.getText().equals("@securityRequirement"));
  private static final Predicate<DetailNode> SECURITY_REQUIREMENT =
      CUSTOM_TAG.and(it -> it.getText().equals("@securityRequirement"));
  private static final Predicate<DetailNode> SECURITY_SCHEME =
      CUSTOM_TAG.and(it -> it.getText().startsWith("@securityScheme."));
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

  public static List<SecurityRequirement> securityRequirement(DetailNode node) {
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
              var scopeList = Stream.of(scopes.split(",")).map(String::trim).toList();
              var security = new SecurityRequirement();
              security.addList(key, scopeList);
              return security;
            })
        .toList();
  }

  public static Map<String, SecurityScheme> securitySchemes(DetailNode node) {
    var result = new LinkedHashMap<String, SecurityScheme>();
    parse(node, SECURITY_SCHEME, "securityScheme")
        .forEach(
            hash -> {
              var item = new SecurityScheme();
              item.setDescription((String) hash.get("description"));
              var name = (String) hash.get("name");
              var paramName = (String) hash.getOrDefault("paramName", name);
              item.setName(paramName);
              ofNullable((String) hash.get("in"))
                  .map(String::toUpperCase)
                  .map(SecurityScheme.In::valueOf)
                  .ifPresent(item::setIn);
              ofNullable((String) hash.get("type"))
                  .map(String::toUpperCase)
                  .map(SecurityScheme.Type::valueOf)
                  .ifPresent(item::setType);
              item.setBearerFormat((String) hash.get("bearerFormat"));
              item.setOpenIdConnectUrl((String) hash.get("openIdConnectUrl"));
              item.setScheme((String) hash.get("scheme"));
              var objectFlows = hash.get("flows");
              if (objectFlows instanceof Map<?, ?> hashFlows) {
                OAuthFlows flows = new OAuthFlows();
                toOauthFlow("implicit", hashFlows, flows::setImplicit);
                toOauthFlow("password", hashFlows, flows::setPassword);
                toOauthFlow("authorizationCode", hashFlows, flows::setAuthorizationCode);
                toOauthFlow("clientCredentials", hashFlows, flows::setClientCredentials);
                item.setFlows(flows);
              }
              result.put(name, item);
            });
    return result;
  }

  private static void toOauthFlow(String path, Map<?, ?> flows, Consumer<OAuthFlow> consumer) {
    var flowHash = flows.get(path);
    if (flowHash instanceof Map hash) {
      var oauthFlow = new OAuthFlow();
      oauthFlow.setAuthorizationUrl((String) hash.get("authorizationUrl"));
      oauthFlow.setTokenUrl((String) hash.get("tokenUrl"));
      oauthFlow.setRefreshUrl((String) hash.get("refreshUrl"));
      var scopesObject = hash.get("scopes");
      List<String> scopeNames;
      List<String> scopeDescriptions;
      if (scopesObject instanceof Map<?, ?> scopesHash) {
        scopeNames = ensureList(scopesHash.get("name"));
        scopeDescriptions = ensureList(scopesHash.get("description"));
      } else {
        scopeNames = ensureList(scopesObject);
        scopeDescriptions = List.of();
      }
      if (!scopeNames.isEmpty()) {
        Scopes scopes = new Scopes();
        for (int i = 0; i < scopeNames.size(); i++) {
          var description = i < scopeDescriptions.size() ? scopeDescriptions.get(i) : "";
          scopes.addString(scopeNames.get(i), description);
        }
        oauthFlow.setScopes(scopes);
      }
      consumer.accept(oauthFlow);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static List<String> ensureList(Object value) {
    if (value == null) return List.of();
    if (value instanceof List list) {
      return list.stream().map(Objects::toString).toList();
    }
    return List.of(value.toString());
  }

  public static List<Server> servers(DetailNode node) {
    return parse(node, SERVER, "server").stream()
        .map(
            hash -> {
              var server = new Server();
              server.setDescription((String) hash.get("description"));
              server.setUrl((String) hash.get("url"));
              return server;
            })
        .toList();
  }

  public static List<Contact> contacts(DetailNode node) {
    return parse(node, CONTACT, "contact").stream()
        .map(
            hash -> {
              var item = new Contact();
              item.setName((String) hash.get("name"));
              item.setUrl((String) hash.get("url"));
              item.setEmail((String) hash.get("email"));
              return item;
            })
        .toList();
  }

  public static List<License> license(DetailNode node) {
    return parse(node, LICENSE, "license").stream()
        .map(
            hash -> {
              var item = new License();
              item.setName((String) hash.get("name"));
              item.setIdentifier((String) hash.get("identifier"));
              item.setUrl((String) hash.get("url"));
              return item;
            })
        .toList();
  }

  private static List<Map<String, Object>> parse(
      DetailNode node, Predicate<DetailNode> filter, String path) {
    var values = new ArrayList<String>();
    javaDocTag(
        node,
        filter,
        (tag, value) -> {
          values.add(tag.getText().substring(1));
          values.add(value);
        });
    return JavaDocObjectParser.parse(values).stream()
        .map(hash -> path == null ? hash : (Map<String, Object>) hash.get(path))
        .toList();
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
    return parse(node, EXTENSION, null).stream().findFirst().orElse(Map.of());
  }

  public static List<Tag> tags(DetailNode node) {
    var tags =
        parse(node, TAG_SHORT, null).stream()
            .map(hash -> hash.get("tag").toString().trim())
            .map(
                value -> {
                  // look first space
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
                  return createTag(name, description);
                })
            .collect(Collectors.toList());
    parse(node, TAG, "tag").stream()
        .map(hash -> createTag((String) hash.get("name"), (String) hash.get("description")))
        .forEach(tags::add);
    return tags;
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
