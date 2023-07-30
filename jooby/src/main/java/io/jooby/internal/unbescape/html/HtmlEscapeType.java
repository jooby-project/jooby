/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.html;

/**
 * Types of escape operations to be performed on HTML text:
 *
 * <ul>
 *   <li><tt><strong>HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL</strong></tt>: Replace escaped
 *       characters with HTML 4 <em>Named Character References</em> (<em>Character Entity
 *       References</em>) whenever possible (depending on the specified {@link HtmlEscapeLevel}),
 *       and default to using <em>Decimal Character References</em> for escaped characters that do
 *       not have an associated NCR.
 *   <li><tt><strong>HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA</strong></tt>: Replace escaped
 *       characters with HTML 4 <em>Named Character References</em> (<em>Character Entity
 *       References</em>) whenever possible (depending on the specified {@link HtmlEscapeLevel}),
 *       and default to using <em>Hexadecimal Character References</em> for escaped characters that
 *       do not have an associated NCR.
 *   <li><tt><strong>HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL</strong></tt>: Replace escaped
 *       characters with HTML5 <em>Named Character References</em> whenever possible (depending on
 *       the specified {@link HtmlEscapeLevel}), and default to using <em>Decimal Character
 *       References</em> for escaped characters that do not have an associated NCR.
 *   <li><tt><strong>HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA</strong></tt>: Replace escaped
 *       characters with HTML5 <em>Named Character References</em> whenever possible (depending on
 *       the specified {@link HtmlEscapeLevel}), and default to using <em>Hexadecimal Character
 *       References</em> for escaped characters that do not have an associated NCR.
 *   <li><tt><strong>DECIMAL_REFERENCES</strong></tt>: Replace escaped characters with <em>Decimal
 *       Character References</em> (will never use NCRs).
 *   <li><tt><strong>HEXADECIMAL_REFERENCES</strong></tt>: Replace escaped characters with
 *       <em>Hexadecimal Character References</em> (will never use NCRs).
 * </ul>
 *
 * <p>For further information, see the <em>Glossary</em> and the <em>References</em> sections at the
 * documentation for the HtmlEscape class.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.0.0
 */
public enum HtmlEscapeType {
  /** Use HTML5 NCRs if possible, default to Decimal Character References. */
  HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL(true, false),

  /** Use HTML5 NCRs if possible, default to Hexadecimal Character References. */
  HTML5_NAMED_REFERENCES_DEFAULT_TO_HEXA(true, true),

  /** Always use Decimal Character References (no NCRs will be used). */
  DECIMAL_REFERENCES(false, false),

  /** Always use Hexadecimal Character References (no NCRs will be used). */
  HEXADECIMAL_REFERENCES(false, true);

  private final boolean useNCRs;
  private final boolean useHexa;

  HtmlEscapeType(final boolean useNCRs, final boolean useHexa) {
    this.useNCRs = useNCRs;
    this.useHexa = useHexa;
  }

  boolean getUseNCRs() {
    return this.useNCRs;
  }

  boolean getUseHexa() {
    return this.useHexa;
  }
}
