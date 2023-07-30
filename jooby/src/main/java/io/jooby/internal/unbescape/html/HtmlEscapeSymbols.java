/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.unbescape.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of this class group all the complex data structures needed to support full escape and
 * unescape operations for HTML.
 *
 * <p>Most of the fields in objects of this class are package-accessible, as the class itself is, in
 * order to allow them (the fields) to be directly accessed from the classes doing the real
 * escape/unescape (basically, the {@link HtmlEscapeUtil} class.
 *
 * @author Daniel Fern&aacute;ndez
 * @since 1.0.0
 */
final class HtmlEscapeSymbols {

  /*
   * GLOSSARY
   * ------------------------
   *
   *   NCR
   *      Named Character Reference or Character Entity Reference: textual
   *      representation of an Unicode codepoint: &aacute;
   *
   *   DCR
   *      Decimal Character Reference: base-10 numerical representation of an Unicode codepoint: &#225;
   *
   *   HCR
   *      Hexadecimal Character Reference: hexadecimal numerical representation of an Unicode codepoint: &#xE1;
   *
   *   Unicode Codepoint
   *      Each of the int values conforming the Unicode code space.
   *      Normally corresponding to a Java char primitive value (codepoint <= \uFFFF),
   *      but might be two chars for codepoints \u10000 to \u10FFFF if the first char is a high
   *      surrogate (\uD800 to \uDBFF) and the second is a low surrogate (\uDC00 to \uDFFF).
   *      See: http://www.oracle.com/technetwork/articles/javase/supplementary-142654.html
   *
   */

  /*
   * Length of the array used for holding the 'base' NCRS indexed by the codepoints themselves. This size
   * (0x2fff - 12287) is considered enough to hold most of the NCRS that should be needed (HTML4 has 252
   * NCRs with a maximum codepoint of 0x2666 - HTML5 has 2125 NCRs with a maximum codepoint of 120171, but
   * only 138 scarcely used NCRs live above codepoint 0x2fff so an overflow map should be enough for
   * those 138 cases).
   */
  static final int NCRS_BY_CODEPOINT_LEN = 0x2fff;

  /*
   * This array will contain the NCRs for the first NCRS_BY_CODEPOINT_LEN (0x2fff) codepoints, indexed by
   * the codepoints themselves so that they (even in the form of mere char's) can be used for array random access.
   * - Values are short in order to index values at the SORTED_NCRS array. This avoids the need for this
   *   array to hold String pointers, which would be 4 bytes in size each (compared to shorts, which are 2 bytes).
   * - Chars themselves or int codepoints can (will, in fact) be used as indexes.
   * - Given values are short, the maximum amount of total references this class can handle is 0x7fff = 32767
   *   (which is safe, because HTML5 has 2125).
   * - All XML and HTML4 NCRs will fit in this array. In the case of HTML5 NCRs, only 138 of the 2125 will
   *   not fit here (NCRs assigned to codepoints > 0x2fff), and an overflow map will be provided for them.
   * - Approximate size will be 16 (header) + 12287 * 2 = 24590 bytes.
   */
  final short[] NCRS_BY_CODEPOINT = new short[NCRS_BY_CODEPOINT_LEN];

  /*
   * This map will work as an overflow of the NCRS_BY_CODEPOINT array, so that the codepoint-to-NCR relation is
   * stored here (with hash-based access) for codepoints >= NCRS_BY_CODEPOINT_LEN (0x2fff).
   * - The use of a Map here still allows for reasonabily fast access for those rare cases in which codepoints above
   *   0x2fff are used.
   * - In the real world, this map will contain the 138 values needed by HTML5 for codepoints >= 0x2fff.
   * - Approximate max size will be (being a complex object like a Map, it's a rough approximation):
   *   16 (header) + 138 * (16 (entry header) + 16*2 (key, value headers) + 4 (key) + 2 (value)) = 7468 bytes
   */
  final Map<Integer, Short>
      NCRS_BY_CODEPOINT_OVERFLOW; // No need to instantiate it until we know it's needed

  /*
   * Maximum char value inside the ASCII plane
   */
  static final char MAX_ASCII_CHAR = 0x7f;

  /*
   * This array will hold the 'escape level' assigned to each ASCII character (codepoint), 0x0 to 0x7f and also
   * a level for the rest of non-ASCII characters.
   * - These levels are used to configure how (and if) escape operations should ignore ASCII or non-ASCII
   *   characters, or escape them somehow if required.
   * - Each HtmlEscapeSymbols structure will define a different set of levels for ASCII chars, according to their needs.
   * - Position 0x7f + 1 represents all the non-ASCII characters. The specified value will determine whether
   *   all non-ASCII characters have to be escaped or not.
   */
  final byte[] ESCAPE_LEVELS = new byte[MAX_ASCII_CHAR + 2];

  /*
   * This array will contain all the NCRs, alphabetically ordered.
   * - Positions in this array will correspond to positions in the SORTED_CODEPOINTS array, so that one array
   *   (this one) holds the NCRs while the other one holds the codepoint(s) such NCRs refer to.
   * - Gives the opportunity to store all NCRs in alphabetical order and therefore be able to perform
   *   binary search operations in order to quickly find NCRs (and translate to codepoints) when unescaping.
   * - Note this array will contain:
   *     * All NCRs referenced from NCRS_BY_CODEPOINT
   *     * NCRs whose codepoint is >= 0x2fff and therefore live in NCRS_BY_CODEPOINT_OVERFLOW
   *     * NCRs which are not referenced in any of the above because they are a shortcut for (and completely
   *       equivalent to) a sequence of two codepoints. These NCRs will only be unescaped, but never escaped.
   * - Max size in real world, when populated for HTML5: 2125 NCRs * 4 bytes/objref -> 8500 bytes, plus the texts.
   */
  final char[][] SORTED_NCRS;

  /*
   * This array contains all the codepoints corresponding to the NCRs stored in SORTED_NCRS. This array is ordered
   * so that each index in SORTED_NCRS can also be used to retrieve the corresponding CODEPOINT when used on this array.
   * - Values in this array can be positive (= single codepoint) or negative (= double codepoint, will need further
   *   resolution by means of the DOUBLE_CODEPOINTS array)
   * - Max size in real world, when populated for HTML5: 2125 NCRs * 4 bytes/objref -> 8500 bytes.
   */
  final int[] SORTED_CODEPOINTS;

  /*
   * This array stores the sequences of two codepoints that are escaped as a single NCR. The indexes of this array are
   * referenced as negative numbers at the SORTED_CODEPOINTS array, and the values are int[2], containing the
   * sequence of codepoints. HTML4 has no NCRs like this, HTML5 has 93.
   * - Note this array is only used in UNESCAPE operations. Double-codepoint NCR escape is not performed because
   *   the resulting characters are exactly equivalent to the escape of the two codepoints separately.
   * - Max size in real world, when populated for HTML5 (rough approximate): 93 * (4 (ref) + 16 + 2 * 4) = 2604 bytes
   */
  final int[][] DOUBLE_CODEPOINTS;

  /*
   * This constant will be used at the NCRS_BY_CODEPOINT array to specify there is no NCR associated with a
   * codepoint.
   */
  static final short NO_NCR = (short) 0;

  /*
   * Constants holding the definition of all the HtmlEscapeSymbols for HTML4 and HTML5, to be used in escape and
   * unescape operations.
   */
  static final HtmlEscapeSymbols HTML5_SYMBOLS;

  static {
    HTML5_SYMBOLS = Html5EscapeSymbolsInitializer.initializeHtml5();
  }

  /*
   * Create a new HtmlEscapeSymbols structure. This will initialize all the structures needed to cover the
   * specified references and escape levels, including sorted arrays, overflow maps, etc.
   */
  HtmlEscapeSymbols(final References references, final byte[] escapeLevels) {

    super();

    // Initialize ASCII escape levels: just copy the array
    System.arraycopy(escapeLevels, 0, ESCAPE_LEVELS, 0, (0x7f + 2));

    // Initialize some auxiliary structures
    final List<char[]> ncrs = new ArrayList<char[]>(references.references.size() + 5);
    final List<Integer> codepoints = new ArrayList<Integer>(references.references.size() + 5);
    final List<int[]> doubleCodepoints = new ArrayList<int[]>(100);
    final Map<Integer, Short> ncrsByCodepointOverflow = new HashMap<Integer, Short>(20);

    // For each reference, initialize its corresponding codepoint -> ncr and ncr -> codepoint
    // structures
    for (final Reference reference : references.references) {

      final char[] referenceNcr = reference.ncr;
      final int[] referenceCodepoints = reference.codepoints;

      ncrs.add(referenceNcr);

      if (referenceCodepoints.length == 1) {
        // Only one codepoint (might be > 1 chars, though), this is the normal case

        final int referenceCodepoint = referenceCodepoints[0];
        codepoints.add(Integer.valueOf(referenceCodepoint));

      } else if (referenceCodepoints.length == 2) {
        // Two codepoints, therefore this NCR will translate when unescaping into a two-codepoint
        // (probably two-char, too) sequence. We will use a negative codepoint value to signal this.

        doubleCodepoints.add(referenceCodepoints);
        // Will need to subtract one from its index when unescaping (codepoint = -1 -> position 0)
        codepoints.add(Integer.valueOf((-1) * doubleCodepoints.size()));

      } else {

        throw new RuntimeException(
            "Unsupported codepoints #: "
                + referenceCodepoints.length
                + " for "
                + new String(referenceNcr));
      }
    }

    // We hadn't touched this array before. First thing to do is initialize it, as it will have a
    // huge
    // amount of "empty" (i.e. non-assigned) values.
    Arrays.fill(NCRS_BY_CODEPOINT, NO_NCR);

    // We can initialize now these arrays that will hold the NCR-to-codepoint correspondence, but we
    // cannot copy
    // them directly from our auxiliary structures because we need to order the NCRs alphabetically
    // first.

    SORTED_NCRS = new char[ncrs.size()][];
    SORTED_CODEPOINTS = new int[codepoints.size()];

    final List<char[]> ncrsOrdered = new ArrayList<char[]>(ncrs);
    Collections.sort(
        ncrsOrdered,
        new Comparator<char[]>() {
          public int compare(final char[] o1, final char[] o2) {
            return HtmlEscapeSymbols.compare(o1, o2, 0, o2.length);
          }
        });

    for (short i = 0; i < SORTED_NCRS.length; i++) {

      final char[] ncr = ncrsOrdered.get(i);
      SORTED_NCRS[i] = ncr;

      for (short j = 0; j < SORTED_NCRS.length; j++) {

        if (Arrays.equals(ncr, ncrs.get(j))) {

          final int cp = codepoints.get(j);
          SORTED_CODEPOINTS[i] = cp;

          if (cp > 0) {
            // Not negative (i.e. not double-codepoint)
            if (cp < NCRS_BY_CODEPOINT_LEN) {
              // Not overflown
              if (NCRS_BY_CODEPOINT[cp] == NO_NCR) {
                // Only the first NCR for each codepoint will be used for escaping.
                NCRS_BY_CODEPOINT[cp] = i;
              } else {
                final int positionOfCurrent =
                    positionInList(ncrs, SORTED_NCRS[NCRS_BY_CODEPOINT[cp]]);
                final int positionOfNew = positionInList(ncrs, ncr);
                if (positionOfNew < positionOfCurrent) {
                  // The order in which NCRs were originally specified in the references argument
                  // marks which NCR should be used for escaping (the first one), if several NCRs
                  // have the same codepoint.
                  NCRS_BY_CODEPOINT[cp] = i;
                }
              }
            } else {
              // Codepoint should be overflown
              ncrsByCodepointOverflow.put(Integer.valueOf(cp), Short.valueOf(i));
            }
          }

          break;
        }
      }
    }

    // Only create the overflow map if it is really needed.
    if (ncrsByCodepointOverflow.size() > 0) {
      NCRS_BY_CODEPOINT_OVERFLOW = ncrsByCodepointOverflow;
    } else {
      NCRS_BY_CODEPOINT_OVERFLOW = null;
    }

    // Finally, the double-codepoints structure can be initialized, if really needed.
    if (doubleCodepoints.size() > 0) {
      DOUBLE_CODEPOINTS = new int[doubleCodepoints.size()][];
      for (int i = 0; i < DOUBLE_CODEPOINTS.length; i++) {
        DOUBLE_CODEPOINTS[i] = doubleCodepoints.get(i);
      }
    } else {
      DOUBLE_CODEPOINTS = null;
    }
  }

  /*
   * Utility method, used for determining which of the different NCRs for the same
   * codepoint (when there are many) was specified first, because that is the one
   * we should be using for escaping.
   * (Note all of the NCRs will be available for unescaping, obviously)
   */
  private static int positionInList(final List<char[]> list, final char[] element) {
    int i = 0;
    for (final char[] e : list) {
      if (Arrays.equals(e, element)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  /*
   * These two methods (two versions: for String and for char[]) compare each of the candidate
   * text fragments with an NCR coming from the SORTED_NCRs array, during binary search operations.
   *
   * Note these methods not only perform a normal comparison (returning -1, 0 or 1), but will also
   * return a negative number < -10 when a partial match is possible, this is, when the specified text
   * fragment contains a complete NCR at its first chars but contains more chars afterwards. This is
   * useful for matching HTML5 NCRs which do not end in ; (like '&aacute'), which will come in bigger fragments
   * because the unescape method will have no way of differentiating the chars after the NCR from chars that
   * could be in fact part of the NCR. Also note that, in the case of a partial match, (-1) * (returnValue + 10)
   * will specify the number of matched chars.
   *
   * Note we will willingly alter order so that ';' goes always first (even before no-char). This will allow
   * proper functioning of the partial-matching mechanism for NCRs that can appear both with and without
   * a ';' suffix.
   */

  private static int compare(final char[] ncr, final String text, final int start, final int end) {
    final int textLen = end - start;
    final int maxCommon = Math.min(ncr.length, textLen);
    int i;
    // char 0 is discarded, will be & in both cases
    for (i = 1; i < maxCommon; i++) {
      final char tc = text.charAt(start + i);
      if (ncr[i] < tc) {
        if (tc == ';') {
          return 1;
        }
        return -1;
      } else if (ncr[i] > tc) {
        if (ncr[i] == ';') {
          return -1;
        }
        return 1;
      }
    }
    if (ncr.length > i) {
      if (ncr[i] == ';') {
        return -1;
      }
      return 1;
    }
    if (textLen > i) {
      if (text.charAt(start + i) == ';') {
        return 1;
      }
      // We have a partial match. Can be an NCR not finishing in a semicolon
      return -((textLen - i) + 10);
    }
    return 0;
  }

  private static int compare(final char[] ncr, final char[] text, final int start, final int end) {
    final int textLen = end - start;
    final int maxCommon = Math.min(ncr.length, textLen);
    int i;
    // char 0 is discarded, will be & in both cases
    for (i = 1; i < maxCommon; i++) {
      final char tc = text[start + i];
      if (ncr[i] < tc) {
        if (tc == ';') {
          return 1;
        }
        return -1;
      } else if (ncr[i] > tc) {
        if (ncr[i] == ';') {
          return -1;
        }
        return 1;
      }
    }
    if (ncr.length > i) {
      if (ncr[i] == ';') {
        return -1;
      }
      return 1;
    }
    if (textLen > i) {
      if (text[start + i] == ';') {
        return 1;
      }
      // We have a partial match. Can be an NCR not finishing in a semicolon
      return -((textLen - i) + 10);
    }
    return 0;
  }

  /*
   * These two methods (two versions: for String and for char[]) are used during unescape at the
   * {@link HtmlEscapeUtil} class in order to quickly find the NCR corresponding to a preselected fragment
   * of text (if there is such NCR).
   *
   * Note this operation supports partial matching (based on the above 'compare(...)' methods). That way,
   * if an exact match is not found but a partial match exists, the partial match will be returned.
   */

  static int binarySearch(
      final char[][] values, final String text, final int start, final int end) {

    int low = 0;
    int high = values.length - 1;

    int partialIndex = Integer.MIN_VALUE;
    int partialValue = Integer.MIN_VALUE;

    while (low <= high) {

      final int mid = (low + high) >>> 1;
      final char[] midVal = values[mid];

      final int cmp = compare(midVal, text, start, end);

      if (cmp == -1) {
        low = mid + 1;
      } else if (cmp == 1) {
        high = mid - 1;
      } else if (cmp < -10) {
        // Partial match
        low = mid + 1;
        if (partialIndex == Integer.MIN_VALUE || partialValue < cmp) {
          partialIndex = mid;
          partialValue =
              cmp; // partial will always be negative, and -10. We look for the smallest partial
        }
      } else {
        // Found!!
        return mid;
      }
    }

    if (partialIndex != Integer.MIN_VALUE) {
      // We have a partial result. We return the closest result index as negative + (-10)
      return (-1) * (partialIndex + 10);
    }

    return Integer.MIN_VALUE; // Not found!
  }

  static int binarySearch(
      final char[][] values, final char[] text, final int start, final int end) {

    int low = 0;
    int high = values.length - 1;

    int partialIndex = Integer.MIN_VALUE;
    int partialValue = Integer.MIN_VALUE;

    while (low <= high) {

      final int mid = (low + high) >>> 1;
      final char[] midVal = values[mid];

      final int cmp = compare(midVal, text, start, end);

      if (cmp == -1) {
        low = mid + 1;
      } else if (cmp == 1) {
        high = mid - 1;
      } else if (cmp < -10) {
        // Partial match
        low = mid + 1;
        if (partialIndex == Integer.MIN_VALUE || partialValue < cmp) {
          partialIndex = mid;
          partialValue =
              cmp; // partial will always be negative, and -10. We look for the smallest partial
        }
      } else {
        // Found!!
        return mid;
      }
    }

    if (partialIndex != Integer.MIN_VALUE) {
      // We have a partial result. We return the closest result index as negative + (-10)
      return (-1) * (partialIndex + 10);
    }

    return Integer.MIN_VALUE; // Not found!
  }

  /*
   * Inner utility classes that model the named character references to be included in an initialized
   * instance of the HtmlEscapeSymbols class.
   */

  static final class References {

    private final List<Reference> references = new ArrayList<Reference>(200);

    References() {
      super();
    }

    void addReference(final int codepoint, final String ncr) {
      this.references.add(new Reference(ncr, new int[] {codepoint}));
    }

    void addReference(final int codepoint0, final int codepoint1, final String ncr) {
      this.references.add(new Reference(ncr, new int[] {codepoint0, codepoint1}));
    }
  }

  private static final class Reference {

    private final char[] ncr;
    private final int[] codepoints;

    private Reference(final String ncr, final int[] codepoints) {
      super();
      this.ncr = ncr.toCharArray();
      this.codepoints = codepoints;
    }
  }
}
