/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;

class HeadersMultiMapTest {

  @Test
  void shouldAddAndGetHeaders() {
    HeadersMultiMap headers = new HeadersMultiMap(null); // Disable validation for pure logic tests

    headers.add("Content-Type", "application/json");
    headers.add(new AsciiString("X-Custom"), new AsciiString("CustomValue"));

    assertEquals(2, headers.size());
    assertFalse(headers.isEmpty());
    assertEquals("application/json", headers.get("Content-Type"));
    assertEquals("CustomValue", headers.get(new AsciiString("X-Custom")));
  }

  @Test
  void shouldAddAndIterateHeaders() {
    HeadersMultiMap headers = new HeadersMultiMap(null); // Disable validation for pure logic tests

    headers.add("Content-Type", "application/json");
    for (Map.Entry<String, String> header : headers) {
      assertEquals("Content-Type", header.getKey());
      assertEquals("application/json", header.getValue());
    }
  }

  @Test
  void shouldSetHeadersOverwritingExisting() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.add("X-Multi", "Value1");
    headers.add("X-Multi", "Value2");

    assertEquals(2, headers.getAll("X-Multi").size());

    headers.set("X-Multi", "Value3");
    List<String> all = headers.getAll("X-Multi");

    assertEquals(1, all.size());
    assertEquals("Value3", all.get(0));
  }

  @Test
  void shouldRemoveHeaders() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.add("A", "1");
    headers.add("B", "2");

    headers.remove("A");

    assertNull(headers.get("A"));
    assertEquals("2", headers.get("B"));
    assertEquals(1, headers.size());
  }

  @Test
  void shouldHandleHashCollisionsAndLinkedListRemoval() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    for (int i = 0; i < 20; i++) {
      headers.add("Key-" + i, "Val-" + i);
    }

    headers.remove("Key-5");
    assertNull(headers.get("Key-5"));
    assertEquals(19, headers.size());

    headers.clear();
    assertTrue(headers.isEmpty());
    assertEquals(0, headers.size());
  }

  @Test
  void shouldParseCsvForContainsValue() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.add("Accept", "text/html, application/xhtml+xml, application/xml;q=0.9");

    assertFalse(headers.contains("Accept", "application/xhtml+xml", true));

    assertTrue(headers.containsValue("Accept", "application/xhtml+xml", true));
    assertTrue(headers.containsValue("Accept", "text/html", true));

    assertTrue(headers.containsValue("Accept", "APPLICATION/XML;Q=0.9", true));
    assertFalse(headers.containsValue("Accept", "APPLICATION/XML;Q=0.9", false));
  }

  @Test
  void shouldAddAndSetIterables() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.add("List", Arrays.asList("A", "B", "C"));
    assertEquals(3, headers.getAll("List").size());

    headers.set("List", Arrays.asList("X", "Y"));
    assertEquals(2, headers.getAll("List").size());
    assertEquals("X", headers.getAll("List").get(0));
  }

  @Test
  void iterableShouldBreakOnNullElement() {
    HeadersMultiMap headers = new HeadersMultiMap(null);

    // Will not throw NullPointerException anymore, it will gracefully break
    headers.add("List", Arrays.asList("A", null, "B"));

    List<String> values = headers.getAll("List");
    assertEquals(1, values.size());
    assertEquals("A", values.get(0));
  }

  @Test
  void shouldThrowNpeOnNullObjectValue() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    assertThrows(NullPointerException.class, () -> headers.add("Key", (Object) null));
  }

  @Test
  void testPrimitiveGetters() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.addInt("X-Int", 42);
    headers.addShort("X-Short", (short) 8);
    headers.add("X-Date", "Wed, 21 Oct 2015 07:28:00 GMT");

    assertEquals(42, headers.getInt("X-Int"));
    assertEquals(42, headers.getInt("X-Int", 0));
    assertEquals(99, headers.getInt("X-Missing", 99));

    assertEquals((short) 8, headers.getShort("X-Short"));
    assertEquals((short) 8, headers.getShort("X-Short", (short) 0));

    assertNotNull(headers.getTimeMillis("X-Date"));
    assertEquals(100L, headers.getTimeMillis("X-Missing-Date", 100L));
  }

  @Test
  void testPrimitiveSetters() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.setInt("X-Int", 100);
    headers.setShort("X-Short", (short) 5);

    assertEquals(100, headers.getInt("X-Int"));
    assertEquals((short) 5, headers.getShort("X-Short"));
  }

  @Test
  void shouldEncodeToByteBuf() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.add("A", "1");
    headers.add(new AsciiString("B"), new AsciiString("2"));

    ByteBuf buf = Unpooled.buffer();
    headers.encode(buf);

    String encoded = buf.toString(io.netty.util.CharsetUtil.US_ASCII);
    assertTrue(encoded.contains("A: 1\r\n"));
    assertTrue(encoded.contains("B: 2\r\n"));

    buf.release();
  }

  @Test
  void testIteratorsAndForEach() {
    HeadersMultiMap headers = new HeadersMultiMap(null);
    headers.add("A", "1");
    headers.add("B", "2");

    Set<String> names = headers.names();
    assertTrue(names.contains("A"));
    assertTrue(names.contains("B"));

    List<Map.Entry<String, String>> entries = headers.entries();
    assertEquals(2, entries.size());

    Iterator<Map.Entry<CharSequence, CharSequence>> charSeqIter = headers.iteratorCharSequence();
    assertTrue(charSeqIter.hasNext());
    assertNotNull(charSeqIter.next());
  }

  @Test
  void mapEntrySetValueShouldUpdateValueSuccessfully() {
    // Re-create the strict environment independently of the system property.
    HeadersMultiMap headers =
        new HeadersMultiMap(
            (name, value) -> {
              if (name == null || name.length() == 0) {
                throw new IllegalArgumentException("empty header name");
              }
            });
    headers.add("Valid-Key", "Valid-Value");

    Map.Entry<String, String> entry = headers.iterator().next();

    // Because we patched MapEntry.setValue to pass "this.key" instead of "",
    // this will successfully pass validation and update the entry.
    String oldVal = entry.setValue("New-Value");

    assertEquals("Valid-Value", oldVal);
    assertEquals("New-Value", entry.getValue());
    assertEquals("New-Value", headers.get("Valid-Key"));
  }
}
