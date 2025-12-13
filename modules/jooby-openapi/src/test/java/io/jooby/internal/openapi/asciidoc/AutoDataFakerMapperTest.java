/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.asciidoc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.core.JsonProcessingException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutoDataFakerMapperTest {

  private AutoDataFakerMapper mapper;

  @BeforeAll
  void setup() {
    // Initialize with a custom override for testing
    mapper = new AutoDataFakerMapper();
    mapper.synonyms(Map.of("sku_id", "ean13"));
  }

  @Test
  void testExactMatchByClassAndField() {
    // Book.title exists in Datafaker
    Supplier<String> generator = mapper.getGenerator("Book", "title", "string", "fail");
    String result = generator.get();

    assertThat(result).isNotEqualTo("fail").isNotEmpty();
  }

  @Test
  void testAuthorSSN() {
    Supplier<String> generator = mapper.getGenerator("Author", "ssn", "string", "fail");
    String result = generator.get();

    assertThat(result).as("SSN format validation").matches("^\\d{3}-\\d{2}-\\d{4}$");
  }

  @Test
  void testGenericMatchByField() {
    // "firstName" is generic, should map to Name.firstName
    Supplier<String> generator = mapper.getGenerator("User", "firstName", "string", "fail");
    String result = generator.get();

    assertThat(result).isNotEqualTo("fail").doesNotContain("fail");
  }

  @Test
  void testSynonymHandling() {
    // "dob" is a synonym for "birthday"
    Supplier<String> generator = mapper.getGenerator("User", "dob", "date", "fail");
    String result = generator.get();

    // Datafaker birthday usually returns a date string
    assertThat(result).isNotEqualTo("fail");
  }

  @Test
  void testSynonymNormalization() {
    // "e-mail" -> normalize -> "email" -> maps to internet().emailAddress()
    Supplier<String> generator = mapper.getGenerator("User", "e-mail", "string", "fail");
    String result = generator.get();

    assertThat(result).contains("@");
  }

  @Test
  void testFieldTypeFallback() {
    // "unknown_field_xyz" does not exist in Faker.
    // It should fallback to "date-time" type logic.
    Supplier<String> generator =
        mapper.getGenerator("Log", "unknown_field_xyz", "date-time", "fail");
    String result = generator.get();

    assertThat(result).isNotEqualTo("fail");
  }

  @Test
  void testFieldTypeUUID() {
    Supplier<String> generator = mapper.getGenerator("Table", "pk_id", "uuid", "fail");
    String result = generator.get();

    assertThat(result).hasSize(36); // UUID length
  }

  @Test
  void testCustomUserSynonym() {
    // We registered "sku_id" -> "ean13" in setup()
    Supplier<String> generator = mapper.getGenerator("Product", "sku_id", "string", "fail");
    String result = generator.get();

    // EAN13 is numbers
    assertThat(result).matches("\\d+");
  }

  @Test
  void testFuzzyMatching() {
    // "customer_email_address" -> contains "email" -> maps to email provider
    Supplier<String> generator =
        mapper.getGenerator("Customer", "customer_email_address", "string", "fail");
    String result = generator.get();

    assertThat(result).contains("@");
  }

  @Test
  void testCompleteFallback() {
    // Nothing matches this
    Supplier<String> generator =
        mapper.getGenerator("Alien", "warp_speed", "unknown_type", "DEFAULT_VALUE");
    String result = generator.get();

    assertThat(result).isEqualTo("DEFAULT_VALUE");
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCapabilityMapStructure() throws JsonProcessingException {
    Map<String, Object> capabilities = mapper.getCapabilityMap();

    // 1. Check Top Level Keys
    assertThat(capabilities).containsKeys("domains", "generics", "types", "synonyms");

    // 2. Check Domains: "book" -> { "title": "faker.book().title()" }
    Map<String, Map<String, String>> domains =
        (Map<String, Map<String, String>>) capabilities.get("domains");
    assertThat(domains).containsKey("book");

    Map<String, String> bookFields = domains.get("book");
    assertThat(bookFields).containsKey("title");
    assertThat(bookFields.get("title")).contains("faker.book().title");

    // 3. Check Generics: "title" -> "faker.book().title()"
    Map<String, String> generics = (Map<String, String>) capabilities.get("generics");
    assertThat(generics).containsKey("firstname");
    assertThat(generics.get("firstname")).contains("faker.name().firstName");

    // 4. Check Types: "uuid" -> description
    Map<String, String> types = (Map<String, String>) capabilities.get("types");
    assertThat(types).containsKey("uuid");
    assertThat(types.get("uuid")).contains("faker.internet().uuid");

    // 5. Check Synonyms
    Map<String, String> synonyms = (Map<String, String>) capabilities.get("synonyms");
    assertThat(synonyms).containsEntry("surname", "lastname");
    assertThat(synonyms).containsEntry("skuid", "ean13");
  }
}
