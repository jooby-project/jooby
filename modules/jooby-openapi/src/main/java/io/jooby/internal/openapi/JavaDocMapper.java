/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.jooby.javadoc.DocTag;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

public final class JavaDocMapper {
  @SuppressWarnings("unchecked")
  public static Map<String, SecurityScheme> toSecuritySchemes(
      Map<String, Map<String, Object>> data) {
    if (data == null || data.isEmpty()) return Map.of();

    var result = new LinkedHashMap<String, SecurityScheme>();

    data.forEach(
        (key, map) -> {
          var item = new SecurityScheme();
          item.setDescription((String) map.get("description"));

          item.setName((String) map.get("paramName"));

          var inStr = (String) map.get("in");
          if (inStr != null) {
            item.setIn(parseEnum(SecurityScheme.In.class, inStr));
          }

          var typeStr = (String) map.get("type");
          if (typeStr != null) {
            item.setType(parseEnum(SecurityScheme.Type.class, typeStr));
          }

          item.setBearerFormat((String) map.get("bearerFormat"));
          item.setOpenIdConnectUrl((String) map.get("openIdConnectUrl"));
          item.setScheme((String) map.get("scheme"));

          var objectFlows = map.get("flows");
          if (objectFlows instanceof Map<?, ?> hashFlows) {
            var flows = new OAuthFlows();
            flows.setImplicit(toOAuthFlow((Map<String, Object>) hashFlows.get("implicit")));
            flows.setPassword(toOAuthFlow((Map<String, Object>) hashFlows.get("password")));
            flows.setAuthorizationCode(
                toOAuthFlow((Map<String, Object>) hashFlows.get("authorizationCode")));
            flows.setClientCredentials(
                toOAuthFlow((Map<String, Object>) hashFlows.get("clientCredentials")));
            item.setFlows(flows);
          }

          result.put(key, item);
        });

    return result;
  }

  @SuppressWarnings("unchecked")
  private static OAuthFlow toOAuthFlow(Map<String, Object> data) {
    if (data == null || data.isEmpty()) {
      return null;
    }

    var flow = new OAuthFlow();
    flow.setAuthorizationUrl((String) data.get("authorizationUrl"));
    flow.setTokenUrl((String) data.get("tokenUrl"));
    flow.setRefreshUrl((String) data.get("refreshUrl"));

    // OpenAPI requires the scopes object to exist, even if empty
    var scopes = new Scopes();

    // Sometimes parsers map it to 'scope' instead of 'scopes' by mistake
    var scopesObj = data.getOrDefault("scopes", data.get("scope"));

    if (scopesObj instanceof Map<?, ?> scopesMap) {
      // Format 1: Parallel arrays (name="[write:pets]", description="[modify pets]")
      if (scopesMap.containsKey("name") && scopesMap.containsKey("description")) {
        var names = parseListString(String.valueOf(scopesMap.get("name")));
        var descs = parseListString(String.valueOf(scopesMap.get("description")));

        for (int i = 0; i < names.size(); i++) {
          var scopeName = names.get(i);
          var scopeDesc = i < descs.size() ? descs.get(i) : "";
          scopes.addString(scopeName, scopeDesc);
        }
      } else {
        // Format 2: Standard Key-Value Map
        // Safeguard: if value is null, use "" to prevent `"null"` string in YAML
        scopesMap.forEach(
            (k, v) -> scopes.addString(String.valueOf(k), v == null ? "" : String.valueOf(v)));
      }
    } else if (scopesObj instanceof Iterable<?> scopesIterable) {
      // Format 3: Iterables/Lists (e.g. ["user:read"] or [{"user:read": ""}])
      for (var item : scopesIterable) {
        if (item instanceof Map<?, ?> mapItem) {
          mapItem.forEach(
              (k, v) -> scopes.addString(String.valueOf(k), v == null ? "" : String.valueOf(v)));
        } else if (item != null) {
          scopes.addString(String.valueOf(item), "");
        }
      }
    } else if (scopesObj instanceof String scopesStr) {
      // Format 5: Raw String array (e.g. "[user:read]")
      for (String scopeName : parseListString(scopesStr)) {
        scopes.addString(scopeName, "");
      }
    }

    flow.setScopes(scopes);
    return flow;
  }

  /** Safely unwraps bracketed comma-separated strings into a List. */
  private static java.util.List<String> parseListString(String val) {
    if (val == null) return java.util.List.of();

    val = val.trim();
    if (val.startsWith("[") && val.endsWith("]")) {
      val = val.substring(1, val.length() - 1);
    }

    return java.util.Arrays.stream(val.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  public static List<Server> toServers(List<Map<String, Object>> data) {
    if (data == null || data.isEmpty()) return List.of();

    return data.stream()
        .map(
            map -> {
              var server = new Server();
              server.setUrl((String) map.get("url"));
              server.setDescription((String) map.get("description"));
              return server;
            })
        .toList();
  }

  public static Optional<Contact> toContact(List<Map<String, Object>> data) {
    if (data == null || data.isEmpty()) return Optional.empty();

    var map = data.getFirst();
    var contact = new Contact();
    contact.setName((String) map.get("name"));
    contact.setUrl((String) map.get("url"));
    contact.setEmail((String) map.get("email"));
    return Optional.of(contact);
  }

  public static Optional<License> toLicense(List<Map<String, Object>> data) {
    if (data == null || data.isEmpty()) return Optional.empty();

    var map = data.getFirst();
    var license = new License();
    license.setName((String) map.get("name"));
    license.setUrl((String) map.get("url"));
    return Optional.of(license);
  }

  public static List<Tag> toTags(List<DocTag> data) {
    if (data == null || data.isEmpty()) return List.of();

    return data.stream()
        .map(
            docTag -> {
              var tag = new Tag();
              tag.setName(docTag.name());
              tag.setDescription(docTag.description());
              return tag;
            })
        .toList();
  }

  public static List<SecurityRequirement> toSecurityRequirements(
      List<Map<String, List<String>>> data) {
    if (data == null || data.isEmpty()) return List.of();

    return data.stream()
        .map(
            map -> {
              var req = new SecurityRequirement();
              map.forEach(req::addList);
              return req;
            })
        .toList();
  }

  /** Helper to safely map string values to Swagger Enums, ignoring case. */
  private static <T extends Enum<T>> T parseEnum(Class<T> type, String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    for (T constant : type.getEnumConstants()) {
      if (constant.name().equalsIgnoreCase(value)) {
        return constant;
      }
    }
    return null;
  }
}
