package org.jooby.raml;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.jooby.Upload;
import org.jooby.internal.raml.RamlType;
import org.junit.Test;

import com.google.inject.util.Types;

public class RamlTypeTest {

  @Test
  public void scalar() {
    assertEquals("type: boolean", RamlType.parse(boolean.class).toString());
    assertEquals("type: boolean", RamlType.parse(Boolean.class).toString());
    assertEquals("type: integer", RamlType.parse(int.class).toString());
    assertEquals("type: integer", RamlType.parse(Integer.class).toString());
    assertEquals("type: number", RamlType.parse(double.class).toString());
    assertEquals("type: number", RamlType.parse(double.class).toString());
    assertEquals("type: string", RamlType.parse(String.class).toString());
    assertEquals("type: string", RamlType.parse(char.class).toString());
    assertEquals("type: integer\nrequired: false", RamlType.parse(optional(Integer.class)).toString());
    assertEquals("type: date", RamlType.parse(Date.class).toString());
    assertEquals("type: date", RamlType.parse(LocalDate.class).toString());
  }

  @Test
  public void file() {
    assertEquals("type: file", RamlType.parse(Upload.class).toString());
  }

  @Test
  public void enums() {
    assertEquals("Freq:\n" +
        "  type: string\n" +
        "  enum: [DAILY, WEEKLY]", RamlType.parse(Freq.class).toString());
  }

  @Test
  public void array() {
    assertEquals("type: integer[]", RamlType.parse(Types.listOf(Integer.class)).toString());
    assertEquals("type: string[]", RamlType.parse(Types.listOf(String.class)).toString());
    assertEquals("type: Person[]", RamlType.parse(Types.listOf(Person.class)).toString());
    assertEquals("type: Person[]\nuniqueItems: true", RamlType.parse(Types.setOf(Person.class)).toString());
  }

  @Test
  public void object() {
    assertEquals("Person:\n" +
        "  type: object\n" +
        "  properties:\n" +
        "    name:\n" +
        "      type: string\n" +
        "    parent:\n" +
        "      type: Person\n" +
        "    children:\n" +
        "      type: Person[]\n" +
        "    age:\n" +
        "      type: integer\n" +
        "      required: false", RamlType.parse(Person.class).toString());
  }

  @Test
  public void uuid() {
    assertEquals("uuid:\n" +
            "  type: string\n" +
            "  pattern: ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", RamlType.parse(UUID.class).toString());
  }

  private Type optional(final Class<?> type) {
    return Types.newParameterizedType(Optional.class, type);
  }
}
