/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoreProjectionTest {

  // --- Helper Classes for Reflection Coverage ---
  public static class User {
    public String getName() {
      return null;
    }

    public boolean isActive() {
      return true;
    }

    public Address getAddress() {
      return null;
    }

    public List<Role> getRoles() {
      return null;
    }

    public Map<String, Profile> getProfiles() {
      return null;
    }

    public String id;
  }

  public static class Address {
    private static final String STATIC_FIELD = "static";
    private transient String transientField;

    public String getCity() {
      return null;
    }

    public boolean isEnabled() {
      return false;
    }

    private String zip;

    public String line1() {
      return null;
    }
  }

  public static class Role {
    public String getLevel() {
      return null;
    }
  }

  public static class Profile {
    public String getBio() {
      return null;
    }
  }

  public static class Circular {
    public Circular getNext() {
      return null;
    }
  }

  public record SimpleRecord(String name, int age) {}

  @Test
  @DisplayName("Test basic inclusion - Matches Insertion Order")
  void testBasicParsing() {
    Projection<User> p = Projection.of(User.class);
    p.include("name", "active", "id");
    assertEquals("(name,active,id)", p.toView());
  }

  @Test
  @DisplayName("Test nested grouping and trimming")
  void testNestedGrouping() {
    Projection<User> p =
        Projection.of(User.class).include(" address ( city, zip ) ", "roles(level)");
    assertEquals("(address(city,zip),roles(level))", p.toView());
  }

  @Test
  @DisplayName("Test deep wildcard")
  void testWildcards() {
    // address(*) triggers buildDeepWildcard which uses TreeMap
    Projection<User> p = Projection.of(User.class).include("address(*)");
    assertEquals("(address(city,enabled,zip))", p.toView());
  }

  @Test
  @DisplayName("Test validation error branches")
  void testValidationErrors() {
    Projection<User> p = Projection.of(User.class).validate();

    assertThrows(IllegalArgumentException.class, () -> p.include("missingField"));
    assertThrows(IllegalArgumentException.class, () -> p.include("id)"));
    assertThrows(IllegalArgumentException.class, () -> p.include("address(city"));
  }

  @Test
  @DisplayName("Test generic unwrapping (Collections/Maps)")
  void testGenerics() {
    Projection<User> p = Projection.of(User.class).include("roles.level", "profiles.bio");
    // Insertion order: roles first, then profiles
    assertEquals("(roles(level),profiles(bio))", p.toView());
  }

  @Test
  @DisplayName("Test circular reference handling")
  void testCircular() {
    // Circular builds 'next', then recursive buildDeepWildcard sees 'next' again.
    // Because the check is at the start of the method, it allows the first level
    // but stops the second, resulting in next(next).
    Projection<Circular> p = Projection.of(Circular.class).include("next");
    assertEquals("next(next)", p.toView());
  }

  @Test
  @DisplayName("Test Record support and simple type logic")
  void testRecordSupport() {
    Projection<SimpleRecord> p = Projection.of(SimpleRecord.class).include("name", "age");
    assertEquals("(name,age)", p.toView());

    // Indirectly hit isSimpleType via rebuild on a primitive/java.lang type
    assertNotNull(p.getChildren().get("age"));
  }

  @Test
  @DisplayName("Test Object/Dynamic Map branches")
  void testDynamicTypes() {
    // java.util.Map will return Object.class, bypassing strict validation
    Projection<Map> p = Projection.of(Map.class).include("any.random.path");
    assertEquals("any(random(path))", p.toView());
  }

  @Test
  @DisplayName("Test Edge Case: Empty Segments and Nulls")
  void testEmptySegments() {
    Projection<User> p = Projection.of(User.class);
    p.include(" ", null, "name", "");
    assertEquals("name", p.toView());

    // Test root-level grouping notation unwrap: "(id, name)"
    p = Projection.of(User.class).include("(id, name)");
    assertEquals("(id,name)", p.toView());
  }

  @Test
  @DisplayName("Test Field fallback and equals/hashCode")
  void testObjectMethodsAndFields() {
    Projection<Address> p1 = Projection.of(Address.class).include("zip");
    Projection<Address> p2 = Projection.of(Address.class).include("zip");

    assertEquals("zip", p1.toView());
    assertEquals(p1, p2);
    assertEquals(p1.hashCode(), p2.hashCode());
    assertTrue(p1.toString().contains("Address"));
    assertNotEquals(p1, null);
  }
}
