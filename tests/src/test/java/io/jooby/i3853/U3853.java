/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3853;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.avaje.jsonb.Json;

@Json
public class U3853 {
  private final String id;
  private final String name;
  private final A3853 address;
  private final List<R3853> roles;
  private final Map<String, String> meta;

  public U3853(String id, String name, A3853 address, List<R3853> roles, Map<String, String> meta) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.roles = roles;
    this.meta = meta;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public A3853 getAddress() {
    return address;
  }

  public List<R3853> getRoles() {
    return roles;
  }

  public Map<String, String> getMeta() {
    return meta;
  }

  public static U3853 createUser() {
    // Nested Location: The Fortress in the Snow (Level 3)
    L3853 fortress = new L3853(80.0, -20.0);

    // Address: Represents the "Dream Layer"
    A3853 dreamLayer = new A3853("Snow Fortress (Level 3)", fortress);

    // Roles: The Extraction Team
    List<R3853> roles =
        List.of(
            new R3853("The Extractor", 10),
            new R3853("The Architect", 9),
            new R3853("The Point Man", 8),
            new R3853("The Forger", 8));

    // Metadata: Mission specs
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("target", "Robert Fischer");
    meta.put("objective", "Inception");
    meta.put("status", "Synchronizing Kicks");

    // Root User: Dom Cobb
    return new U3853("cobb-001", "Dom Cobb", dreamLayer, roles, meta);
  }
}
