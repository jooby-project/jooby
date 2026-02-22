/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3853;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.avaje.jsonb.Json;
import io.jooby.Extension;
import io.jooby.Projected;
import io.jooby.Projection;
import io.jooby.avaje.jsonb.AvajeJsonbModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3853 {

  Projection<User> STUB = Projection.of(User.class).include("(id, name)");

  @ServerTest
  public void shouldProjectJackson2Data(ServerTestRunner runner) {
    shouldProjectData(runner, new JacksonModule());
  }

  @ServerTest
  public void shouldProjectJackson3Data(ServerTestRunner runner) {
    shouldProjectData(runner, new Jackson3Module());
  }

  @ServerTest
  public void shouldProjectAvajeData(ServerTestRunner runner) {
    shouldProjectData(runner, new AvajeJsonbModule());
  }

  public void shouldProjectData(ServerTestRunner runner, Extension extension) {
    runner
        .define(
            app -> {
              app.install(extension);

              app.get(
                  "/stub",
                  ctx -> {
                    return Projected.wrap(createUser(), STUB);
                  });
              app.get(
                  "/stub/meta",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, meta(target))");
                  });
              app.get(
                  "/stub/roles",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, roles(name))");
                  });
              app.get(
                  "/stub/address",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, name, address(*))");
                  });
              app.get(
                  "/stub/address-stub",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, name, address(city))");
                  });
              app.get(
                  "/stub/address-loc-lat",
                  ctx -> {
                    return Projected.wrap(createUser()).include("(id, name, address(loc(lat)))");
                  });
              app.get(
                  "/stub/address-stub-ref",
                  ctx -> {
                    return Projected.wrap(createUser())
                        .include(User::getId, User::getName)
                        .include(User::getAddress, addr -> addr.include(Address::getCity));
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/stub/meta",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","meta":{"target":"Robert Fischer","objective":"Inception","status":"Synchronizing Kicks"}}
                            """);
                  });
              http.get(
                  "/stub",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb"}
                            """);
                  });
              http.get(
                  "/stub/address",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)","loc":{"lat":80.0,"lon":-20.0}}}
                            """);
                  });
              http.get(
                  "/stub/address-stub",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)"}}
                            """);
                  });
              http.get(
                  "/stub/address-stub-ref",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)"}}
                            """);
                  });
              http.get(
                  "/stub/address-loc-lat",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"loc":{"lat":80.0}}}
                            """);
                  });
              http.get(
                  "/stub/roles",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","roles":[{"name":"The Extractor"},{"name":"The Architect"},{"name":"The Point Man"},{"name":"The Forger"}]}
                            """);
                  });
            });
  }

  @ServerTest
  public void jackson2ShouldNotThrowInvalidDefinitionException(ServerTestRunner runner) {
    jacksonShouldNotThrowInvalidDefinitionException(runner, new JacksonModule());
  }

  @ServerTest
  public void jackson3ShouldNotThrowInvalidDefinitionException(ServerTestRunner runner) {
    jacksonShouldNotThrowInvalidDefinitionException(runner, new Jackson3Module());
  }

  public void jacksonShouldNotThrowInvalidDefinitionException(
      ServerTestRunner runner, Extension extension) {
    runner
        .define(
            app -> {
              app.install(extension);
              app.get(
                  "/user",
                  ctx -> {
                    return createUser();
                  });
            })
        .ready(
            http -> {
              http.get(
                  "/user",
                  rsp -> {
                    assertThat(rsp.body().string())
                        .isEqualToIgnoringNewLines(
                            """
                            {"id":"cobb-001","name":"Dom Cobb","address":{"city":"Snow Fortress (Level 3)","loc":{"lat":80.0,"lon":-20.0}},"roles":[{"name":"The Extractor","level":10},{"name":"The Architect","level":9},{"name":"The Point Man","level":8},{"name":"The Forger","level":8}],"meta":{"target":"Robert Fischer","objective":"Inception","status":"Synchronizing Kicks"}}
                            """);
                  });
            });
  }

  @Json
  public static class User {
    private final String id;
    private final String name;
    private final Address address;
    private final List<Role> roles;
    private final Map<String, String> meta;

    public User(
        String id, String name, Address address, List<Role> roles, Map<String, String> meta) {
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

    public Address getAddress() {
      return address;
    }

    public List<Role> getRoles() {
      return roles;
    }

    public Map<String, String> getMeta() {
      return meta;
    }
  }

  @Json
  public static class Address {
    private final String city;
    private final Location loc;

    public Address(String city, Location loc) {
      this.city = city;
      this.loc = loc;
    }

    public String getCity() {
      return city;
    }

    public Location getLoc() {
      return loc;
    }
  }

  @Json
  public record Role(String name, int level) {}

  @Json
  public record Location(double lat, double lon) {}

  public static User createUser() {
    // Nested Location: The Fortress in the Snow (Level 3)
    Location fortress = new Location(80.0, -20.0);

    // Address: Represents the "Dream Layer"
    Address dreamLayer = new Address("Snow Fortress (Level 3)", fortress);

    // Roles: The Extraction Team
    List<Role> roles =
        List.of(
            new Role("The Extractor", 10),
            new Role("The Architect", 9),
            new Role("The Point Man", 8),
            new Role("The Forger", 8));

    // Metadata: Mission specs
    Map<String, String> meta = new LinkedHashMap<>();
    meta.put("target", "Robert Fischer");
    meta.put("objective", "Inception");
    meta.put("status", "Synchronizing Kicks");

    // Root User: Dom Cobb
    return new User("cobb-001", "Dom Cobb", dreamLayer, roles, meta);
  }
}
