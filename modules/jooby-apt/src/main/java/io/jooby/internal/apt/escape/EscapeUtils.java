/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt.escape;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EscapeUtils {

  /**
   * A Map&lt;CharSequence, CharSequence&gt; to escape the Java control characters.
   *
   * <p>Namely: {@code \b \n \t \f \r}
   */
  private static final Map<CharSequence, CharSequence> JAVA_CTRL_CHARS_ESCAPE;

  static {
    final Map<CharSequence, CharSequence> initialMap = new HashMap<>();
    initialMap.put("\b", "\\b");
    initialMap.put("\n", "\\n");
    initialMap.put("\t", "\\t");
    initialMap.put("\f", "\\f");
    initialMap.put("\r", "\\r");
    JAVA_CTRL_CHARS_ESCAPE = Collections.unmodifiableMap(initialMap);
  }

  /**
   * Translator object for escaping Java.
   *
   * <p>While {@link #escapeJava(String)} is the expected method of use, this object allows the Java
   * escaping functionality to be used as the foundation for a custom translator.
   */
  static final CharSequenceTranslator ESCAPE_JAVA;

  static {
    final Map<CharSequence, CharSequence> escapeJavaMap = new HashMap<>();
    escapeJavaMap.put("\"", "\\\"");
    escapeJavaMap.put("\\", "\\\\");
    ESCAPE_JAVA =
        new AggregateTranslator(
            new LookupTranslator(Collections.unmodifiableMap(escapeJavaMap)),
            new LookupTranslator(JAVA_CTRL_CHARS_ESCAPE),
            JavaUnicodeEscaper.outsideOf(32, 0x7f));
  }

  public static CharSequence escapeJava(CharSequence text) {
    return ESCAPE_JAVA.translate(text);
  }
}
