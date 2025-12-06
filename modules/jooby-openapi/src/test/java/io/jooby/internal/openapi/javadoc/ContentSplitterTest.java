/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi.javadoc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContentSplitterTest {

  @Test
  void shouldHandleNullAndEmpty() {
    assertSplit(null, "", "");
    assertSplit("", "", "");
    assertSplit("   ", "", "");
  }

  @Test
  void shouldSplitOnSimplePeriod() {
    assertSplit("Hello world. This is description.", "Hello world.", "This is description.");
  }

  @Test
  void shouldSplitOnParagraphTag() {
    // <p> acts as the separator, exclusive
    assertSplit("Hello world<p>Description</p>", "Hello world", "Description");

    // Case insensitive <P>
    assertSplit("Hello world<P>Description</P>", "Hello world", "Description");

    assertSplit(
        "This is the Hello <code>/endpoint</code>\n<p>Operation description",
        "This is the Hello <code>/endpoint</code>",
        "Operation description");
  }

  @Test
  void shouldPrioritizeWhateverComesFirst() {
    // Period comes first
    assertSplit("Summary first. Then <p>para</p>.", "Summary first.", "Then para.");

    // Paragraph comes first
    assertSplit(
        "Summary <p>with description containing.</p> periods.",
        "Summary",
        "with description containing. periods.");
  }

  @Test
  void shouldIgnorePeriodInsideParentheses() {
    assertSplit("Jooby (v3.0) is great. Description.", "Jooby (v3.0) is great.", "Description.");

    // Nested parens
    assertSplit("Text (outer (inner.)) done. Desc.", "Text (outer (inner.)) done.", "Desc.");
  }

  @Test
  void shouldIgnorePeriodInsideBrackets() {
    assertSplit("Reference [fig. 1] is here. Next.", "Reference [fig. 1] is here.", "Next.");
  }

  @Test
  void shouldIgnorePeriodInsideHtmlAttributes() {
    assertSplit(
        "Check <a href=\"jooby.io\">site</a>. Done.",
        "Check <a href=\"jooby.io\">site</a>.",
        "Done.");
  }

  @Test
  void shouldHandleComplexHtmlAttributesInP() {
    // <p> with attributes should still trigger split
    assertSplit("Summary<p class=\"lead\">Description</p>", "Summary", "Description");
  }

  @Test
  void shouldNotSplitOnSimilarTags() {
    // <pre> starts with p but is not a paragraph
    assertSplit(
        "Code <pre>val x = 1.0</pre> is cool. End.",
        "Code <pre>val x = 1.0</pre> is cool.",
        "End.");

    // <param> starts with p
    assertSplit(
        "Config <param name='x.y'> ignored. Real split.",
        "Config <param name='x.y'> ignored.",
        "Real split.");
  }

  @Test
  void shouldHandleUnbalancedNestingGracefully() {
    // If user forgets to close (, we probably shouldn't crash,
    // though behavior on period ignore depends on implementation.
    // Logic: if depth > 0, we ignore periods.
    assertSplit("Unbalanced ( paren. No split here.", "Unbalanced ( paren. No split here.", "");

    // Unbalanced closed ) should not make depth negative
    assertSplit("Unbalanced ) paren. Split.", "Unbalanced ) paren.", "Split.");
  }

  @Test
  void shouldHandleNoSeparators() {
    String text = "Just a single sentence without periods or tags";
    assertSplit(text, text, "");
  }

  @Test
  void shouldHandleLeadingAndTrailingSeparators() {
    // Starts with <p> -> Empty summary
    assertSplit("<p>Description only.</p>", "", "Description only.");

    // Ends with period -> Empty description
    assertSplit("Only summary.", "Only summary.", "");
  }

  @Test
  void shouldNotSplitInsidePreTags() {
    // The period in 1.0 must be ignored because it is inside <pre>...</pre>
    assertSplit(
        "Code <pre>val x = 1.0</pre> is cool. End.",
        "Code <pre>val x = 1.0</pre> is cool.",
        "End.");
  }

  @Test
  void shouldNotSplitInsideCodeTags() {
    // The period in System.out must be ignored because it is inside <code>...</code>
    assertSplit(
        "Use <code>System.out.println</code> for logging. Next.",
        "Use <code>System.out.println</code> for logging.",
        "Next.");
  }

  @Test
  void shouldHandleMixedNesting() {
    // Parentheses + Code block
    assertSplit(
        "Check (e.g. <code>var x = 1.0</code>). Done.",
        "Check (e.g. <code>var x = 1.0</code>).",
        "Done.");
  }

  @Test
  void shouldIgnorePeriodInsideJavadocTags() {
    // Test {@code ...}
    assertSplit("Use {@code 1.0} version. Next.", "Use {@code 1.0} version.", "Next.");

    // Test {@link ...}
    assertSplit("See {@link java.util.List}. End.", "See {@link java.util.List}.", "End.");
  }

  @Test
  void shouldIgnorePeriodInsideGeneralBraces() {
    // Since we implemented brace tracking, this also supports standard JSON/Code blocks
    assertSplit(
        "Config { val x = 1.0; } allowed. Next.", "Config { val x = 1.0; } allowed.", "Next.");
  }

  // Helper method to make tests readable
  private void assertSplit(String input, String expectedSummary, String expectedDesc) {
    var result = ContentSplitter.split(input);
    assertEquals(expectedSummary, result.summary(), "Summary mismatch");
    assertEquals(expectedDesc, result.description(), "Description mismatch");
  }
}
