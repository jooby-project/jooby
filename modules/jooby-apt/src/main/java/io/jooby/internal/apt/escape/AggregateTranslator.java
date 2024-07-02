/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.escape;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Executes a sequence of translators one after the other. Execution ends whenever the first
 * translator consumes code points from the input.
 *
 * @since 1.0
 */
class AggregateTranslator extends CharSequenceTranslator {

  /** Translator list. */
  private final List<CharSequenceTranslator> translators = new ArrayList<>();

  /**
   * Specify the translators to be used at creation time.
   *
   * @param translators CharSequenceTranslator array to aggregate
   */
  public AggregateTranslator(final CharSequenceTranslator... translators) {
    if (translators != null) {
      Stream.of(translators) //
          .flatMap(t -> Stream.<CharSequenceTranslator>ofNullable(t)) //
          .forEach(this.translators::add);
    }
  }

  /**
   * The first translator to consume code points from the input is the 'winner'. Execution stops
   * with the number of consumed code points being returned. {@inheritDoc}
   */
  @Override
  public int translate(final CharSequence input, final int index, final Writer writer)
      throws IOException {
    for (final CharSequenceTranslator translator : translators) {
      final int consumed = translator.translate(input, index, writer);
      if (consumed != 0) {
        return consumed;
      }
    }
    return 0;
  }
}
