/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.escape;

import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates a value using a lookup table.
 *
 * @since 1.0
 */
class LookupTranslator extends CharSequenceTranslator {

  /** The mapping to be used in translation. */
  private final Map<String, String> lookupMap;

  /** The first character of each key in the lookupMap. */
  private final BitSet prefixSet;

  /** The length of the shortest key in the lookupMap. */
  private final int shortest;

  /** The length of the longest key in the lookupMap. */
  private final int longest;

  /**
   * Constructs the lookup table to be used in translation
   *
   * <p>Note that, as of Lang 3.1 (the origin of this code), the key to the lookup table is
   * converted to a java.lang.String. This is because we need the key to support hashCode and
   * equals(Object), allowing it to be the key for a HashMap. See LANG-882.
   *
   * @param lookupMap Map&lt;CharSequence, CharSequence&gt; table of translator mappings
   */
  public LookupTranslator(final Map<CharSequence, CharSequence> lookupMap) {
    this.lookupMap = new HashMap<>();
    this.prefixSet = new BitSet();
    int currentShortest = Integer.MAX_VALUE;
    int currentLongest = 0;

    for (final Map.Entry<CharSequence, CharSequence> pair : lookupMap.entrySet()) {
      this.lookupMap.put(pair.getKey().toString(), pair.getValue().toString());
      this.prefixSet.set(pair.getKey().charAt(0));
      final int sz = pair.getKey().length();
      if (sz < currentShortest) {
        currentShortest = sz;
      }
      if (sz > currentLongest) {
        currentLongest = sz;
      }
    }
    this.shortest = currentShortest;
    this.longest = currentLongest;
  }

  /** {@inheritDoc} */
  @Override
  public int translate(final CharSequence input, final int index, final Writer writer)
      throws IOException {
    // check if translation exists for the input at position index
    if (prefixSet.get(input.charAt(index))) {
      int max = longest;
      if (index + longest > input.length()) {
        max = input.length() - index;
      }
      // implement greedy algorithm by trying maximum match first
      for (int i = max; i >= shortest; i--) {
        final CharSequence subSeq = input.subSequence(index, index + i);
        final String result = lookupMap.get(subSeq.toString());

        if (result != null) {
          writer.write(result);
          return Character.codePointCount(subSeq, 0, subSeq.length());
        }
      }
    }
    return 0;
  }
}
