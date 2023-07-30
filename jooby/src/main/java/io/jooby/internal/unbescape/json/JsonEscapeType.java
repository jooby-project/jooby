/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.json;

/**
 * Types of escape operations to be performed on JSON text:
 *
 * <ul>
 *   <li><tt><strong>SINGLE_ESCAPE_CHARS_DEFAULT_TO_UHEXA</strong></tt>: Use Single Escape Chars
 *       whenever possible (depending on the specified {@link JsonEscapeLevel}). For escaped
 *       characters that do not have an associated SEC, default to using <tt>&#92;uFFFF</tt>
 *       Hexadecimal Escapes.
 *   <li><tt><strong>UHEXA</strong></tt>: Replace escaped characters with <tt>&#92;uFFFF</tt>
 *       Hexadecimal Escapes.
 * </ul>
 *
 * <p>For further information, see the <em>Glossary</em> and the <em>References</em> sections at the
 * documentation for the {@link JsonEscape} class.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.0.0
 */
public enum JsonEscapeType {

  /** Use Single Escape Chars if possible, default to &#92;uFFFF hexadecimal escapes. */
  SINGLE_ESCAPE_CHARS_DEFAULT_TO_UHEXA(true),

  /** Always use &#92;uFFFF hexadecimal escapes. */
  UHEXA(false);

  private final boolean useSECs;

  JsonEscapeType(final boolean useSECs) {
    this.useSECs = useSECs;
  }

  boolean getUseSECs() {
    return this.useSECs;
  }
}
