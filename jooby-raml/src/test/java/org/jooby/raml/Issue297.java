package org.jooby.raml;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jooby.internal.raml.RamlType;
import org.junit.Test;

import com.google.inject.util.Types;

public class Issue297 {

  @Test
  public void upperBoundType() {
    assertEquals("type: integer", RamlType.parse(Types.subtypeOf(Integer.class)).toString());

    assertEquals("type: integer[]",
        RamlType.parse(Types.listOf(Types.subtypeOf(Integer.class))).toString());
  }

  @Test
  public void lowerBoundType() {
    assertEquals("type: integer", RamlType.parse(Types.supertypeOf(Integer.class)).toString());

    assertEquals("type: integer[]",
        RamlType.parse(Types.listOf(Types.supertypeOf(Integer.class))).toString());
  }

  @Test
  public void rawList() {
    assertEquals("type: object[]", RamlType.parse(List.class).toString());
  }

  @Test
  public void typeVariable() {
    assertEquals("TVar:\n" +
        "  type: object\n" +
        "  properties:\n" +
        "    items:\n" +
        "      type: Object[]", RamlType.parse(TVar.class).toString());

    assertEquals("TVar2:\n" +
        "  type: object\n" +
        "  properties:\n" +
        "    items:\n" +
        "      type: Number[]\n" +
        "    array:\n" +
        "      type: Number[]", RamlType.parse(TVar2.class).toString());

    assertEquals("type: integer[]", RamlType.parse(Integer[].class).toString());

  }

}
