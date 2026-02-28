/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

/** Tests for Jooby Projection API. */
public class ProjectionTest {

  // --- Test Models ---

  public static class User {
    private String id;
    private String name;
    private Address address;
    private List<Role> roles;
    private Map<String, String> meta;

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

  public static class ExtendedUser extends User {
    public String getFullName() {
      return getName();
    }
  }

  public static class NamedGroup {
    private String name;

    private Group group;

    public String getName() {
      return name;
    }

    public Group getGroup() {
      return group;
    }
  }

  public static class Group {
    private List<ExtendedUser> users;

    public List<ExtendedUser> getUsers() {
      return users;
    }
  }

  public static class Address {
    private String city;
    private Location loc;

    public String getCity() {
      return city;
    }

    public Location getLoc() {
      return loc;
    }
  }

  public record Role(String name, int level) {}

  public record Location(double lat, double lon) {}

  // --- Tests ---

  @Test
  public void testOrderPreservation() {
    // LinkedMap should preserve the order 'name' then 'id'
    Projection<User> p = Projection.of(User.class).include("name", "id");
    assertEquals("(name,id)", p.toView());

    // Swapping order should result in swapped view
    Projection<User> p2 = Projection.of(User.class).include("id", "name");
    assertEquals("(id,name)", p2.toView());
  }

  @Test
  public void testAvajeNotationRoot() {
    // Root level should be wrapped in parentheses for Avaje
    Projection<User> p = Projection.of(User.class).include("id, address(city, loc(lat, lon))");

    assertEquals("(id,address(city,loc(lat,lon)))", p.toView());
  }

  @Test
  public void testInherited() {
    Projection<NamedGroup> group = Projection.of(NamedGroup.class).include("id, group(*)");

    assertEquals(
        "(id,group(users(address(city,loc(lat,lon)),fullName,id,meta,name,roles(level,name))))",
        group.toView());

    Projection<ExtendedUser> p = Projection.of(ExtendedUser.class).include("id, fullname");

    assertEquals("(id,fullname)", p.toView());
  }

  @Test
  public void testMixedNotationRecursive() {
    // Validates that nested children still use parentheses
    Projection<User> p = Projection.of(User.class).include("address.loc(lat, lon)", "roles(name)");

    assertEquals("(address(loc(lat,lon)),roles(name))", p.toView());
  }

  @Test
  public void testTypeSafeInclude() {
    // Type-safe references also follow the defined order
    Projection<User> p = Projection.of(User.class).include(User::getName, User::getId);
    assertEquals("(name,id)", p.toView());
    assertEquals("User(name,id)", p.toString());
  }

  @Test
  public void testCollectionGenericUnwrapping() {
    Projection<User> p = Projection.of(User.class).include("roles.name");
    assertEquals("roles(name)", p.toView());
  }

  @Test
  public void testMapGenericUnwrapping() {
    // Maps resolve to their value type (String in this case)
    assertEquals("meta(bytes)", Projection.of(User.class).include("meta.bytes").toView());
    assertEquals(
        "(id,meta(target))", Projection.of(User.class).include("(id, meta(target))").toView());
  }

  @Test
  public void testRecordSupport() {
    Projection<Role> p = Projection.of(Role.class).include("name", "level");
    assertEquals("(name,level)", p.toView());
  }

  @Test
  public void testFailFastValidation() {
    // Ensures we still blow up on typos during pre-compilation
    assertThrows(
        IllegalArgumentException.class,
        () -> Projection.of(User.class).validate().include("address(ctiy)"));
  }

  @Test
  public void testRootParenthesesBug() {
    assertEquals(
        "(name,address(city))",
        Projection.of(User.class).include("(name, address(city))").toView());

    // Address expands to its deep explicit wildcard definition for Avaje
    assertEquals(
        "(name,address(city,loc(lat,lon)))",
        Projection.of(User.class).include("(name, address)").toView());
  }

  @Test
  public void testRootParentheses() {
    Projection<User> p = Projection.of(User.class).include("(id, name, address)");

    assertTrue(p.getChildren().containsKey("id"));
    assertTrue(p.getChildren().containsKey("name"));
    assertTrue(p.getChildren().containsKey("address"));

    // Address should have no explicitly defined children initially
    // (the deep wildcard happens during toView())
    assertTrue(p.getChildren().get("address").getChildren().isEmpty());

    // Test the expanded view
    assertEquals("(id,name,address(city,loc(lat,lon)))", p.toView());
  }

  @Test
  public void testAvajeWildcardSyntax() {
    Projection<User> p = Projection.of(User.class).include("id, name, address(*)");

    assertTrue(p.getChildren().containsKey("id"));
    assertTrue(p.getChildren().containsKey("name"));
    assertTrue(p.getChildren().containsKey("address"));

    // The explicit '*' should result in an empty children map for address
    assertTrue(p.getChildren().get("address").getChildren().isEmpty());

    // Test the expanded view
    assertEquals("(id,name,address(city,loc(lat,lon)))", p.toView());
  }

  @Test
  public void testNestedWildcardSyntax() {
    Projection<User> p = Projection.of(User.class).include("id, address(city, loc(*))");

    assertTrue(p.getChildren().containsKey("id"));
    assertTrue(p.getChildren().containsKey("address"));

    Projection<?> addressProj = p.getChildren().get("address");
    assertTrue(addressProj.getChildren().containsKey("city"));
    assertTrue(addressProj.getChildren().containsKey("loc"));

    // loc(*) should result in an empty children map for loc
    assertTrue(addressProj.getChildren().get("loc").getChildren().isEmpty());

    // Test the expanded view (loc expands to its fields)
    assertEquals("(id,address(city,loc(lat,lon)))", p.toView());
  }

  @Test
  public void testCollectionNestedSyntax() {
    // Tests: (id, roles(name))
    Projection<User> p = Projection.of(User.class).include("(id, roles(name))");

    assertTrue(p.getChildren().containsKey("id"));
    assertTrue(p.getChildren().containsKey("roles"));

    Projection<?> rolesProj = p.getChildren().get("roles");
    assertTrue(rolesProj.getChildren().containsKey("name"));

    assertEquals("(id,roles(name))", p.toView());
  }

  @Test
  public void testMissingClosingParenthesis() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Projection.of(User.class).include("(id, name, address(*)");
            });

    assertEquals(
        "Missing closing parenthesis in projection: (id, name, address(*)", ex.getMessage());
  }

  @Test
  public void testExtraClosingParenthesis() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              Projection.of(User.class).include("address(city))");
            });

    assertEquals("Mismatched parentheses in projection: address(city))", ex.getMessage());
  }

  @Test
  public void testCollectionDeepWildcardSyntax() {
    // Test: Requesting the 'roles' list without explicitly defining its children.
    // The projection engine should see that 'roles' is a List<Role>,
    // extract the Role class, and expand it to its explicit fields (name, level) for Avaje.
    Projection<User> p = Projection.of(User.class).include("(id, roles)");

    assertTrue(p.getChildren().containsKey("id"));
    assertTrue(p.getChildren().containsKey("roles"));

    // The 'roles' node itself has no explicitly parsed children
    assertTrue(p.getChildren().get("roles").getChildren().isEmpty());

    // But the resulting view string should be fully expanded!
    assertEquals("(id,roles(level,name))", p.toView());
  }

  @Test
  public void testExplicitCollectionDeepWildcardSyntax() {
    // Test: Requesting the 'roles' list using the explicit (*) syntax.
    Projection<User> p = Projection.of(User.class).include("id, roles(*)");

    assertTrue(p.getChildren().containsKey("id"));
    assertTrue(p.getChildren().containsKey("roles"));

    // The resulting view string should be fully expanded!
    assertEquals("(id,roles(level,name))", p.toView());
  }

  @Test
  public void testValidateToggle() {
    // 1. Verify default strict behavior (throws exception)
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Projection.of(User.class).validate().include("address(zipcode)"));
    assertTrue(ex.getMessage().contains("zipcode"));

    // 2. Verify polymorphic/unknown fields are accepted when flag is false
    Projection<User> p =
        Projection.of(User.class).include("address(zipcode), extraPolymorphicField");

    // The projection shouldn't throw, and it should successfully generate the explicit paths
    assertEquals("(address(zipcode),extraPolymorphicField)", p.toView());

    // Verify the internal tree mapped them correctly as generic leaves
    assertTrue(p.getChildren().containsKey("extraPolymorphicField"));
    assertTrue(p.getChildren().get("address").getChildren().containsKey("zipcode"));
  }

  @Test
  public void testTopLevelListProjection() {
    // If your route returns a List<User>, the root projection type is just User.class.
    // The JSON engines (Jackson/Avaje) will naturally apply this User projection
    // to every element in the JSON array.
    Projection<User> projection = Projection.of(User.class).include("id, email");

    // Assert: Avaje view string
    assertEquals("(id,email)", projection.toView());

    // Assert: Tree Structure validates against User
    assertEquals(User.class, projection.getType());
    assertEquals(2, projection.getChildren().size());
    assertTrue(projection.getChildren().containsKey("id"));
    assertTrue(projection.getChildren().containsKey("email"));
    assertFalse(projection.getChildren().containsKey("name"));
  }
}
