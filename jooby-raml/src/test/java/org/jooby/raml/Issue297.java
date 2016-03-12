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

    assertEquals("type: integer[]", RamlType.parse(Types.listOf(Types.subtypeOf(Integer.class))).toString());
  }

  @Test
  public void lowerBoundType() {
    assertEquals("type: integer", RamlType.parse(Types.supertypeOf(Integer.class)).toString());

    assertEquals("type: integer[]", RamlType.parse(Types.listOf(Types.supertypeOf(Integer.class))).toString());
  }

  @Test
  public void rawList() {
    assertEquals("type: object[]", RamlType.parse(List.class).toString());
  }

}
