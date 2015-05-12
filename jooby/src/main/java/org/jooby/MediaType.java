/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * An immutable implementation of HTTP media types (a.k.a mime types).
 *
 * @author edgar
 * @since 0.1.0
 */
public class MediaType implements Comparable<MediaType> {

  /**
   * A media type matcher.
   *
   * @see MediaType#matcher(org.jooby.MediaType)
   * @see MediaType#matcher(java.util.List)
   */
  public static class Matcher {

    /**
     * The source of media types.
     */
    private Iterable<MediaType> acceptable;

    /**
     * Creates a new {@link Matcher}.
     *
     * @param acceptable The source to compare with.
     */
    Matcher(final Iterable<MediaType> acceptable) {
      this.acceptable = acceptable;
    }

    /**
     * Given:
     *
     * <pre>
     *   text/html, application/xhtml; {@literal *}/{@literal *}
     * </pre>
     *
     * <pre>
     *   matches(text/html)        // true through text/html
     *   matches(application/json) // true through {@literal *}/{@literal *}
     * </pre>
     *
     * @param candidate A candidate media type. Required.
     * @return True if the matcher matches the given media type.
     */
    public boolean matches(final MediaType candidate) {
      requireNonNull(candidate, "A candidate media type is required.");
      return doFirst(ImmutableList.of(candidate)) != null;
    }

    /**
     * Given:
     *
     * <pre>
     *   text/html, application/xhtml; {@literal *}/{@literal *}
     * </pre>
     *
     * <pre>
     *   matches(text/html)        // true through text/html
     *   matches(application/json) // true through {@literal *}/{@literal *}
     * </pre>
     *
     * @param candidates One ore more candidates media type. Required.
     * @return True if the matcher matches the given media type.
     */
    public boolean matches(final List<MediaType> candidates) {
      return filter(candidates).size() > 0;
    }

    /**
     * Given:
     *
     * <pre>
     *   text/html, application/xhtml; {@literal *}/{@literal *}
     * </pre>
     *
     * <pre>
     *   first(text/html)        // returns text/html
     *   first(application/json) // returns application/json
     * </pre>
     *
     * @param candidate A candidate media type. Required.
     * @return A first most relevant media type or an empty optional.
     */
    public Optional<MediaType> first(final MediaType candidate) {
      return first(ImmutableList.of(candidate));
    }

    /**
     * Given:
     *
     * <pre>
     *   text/html, application/xhtml; {@literal *}/{@literal *}
     * </pre>
     *
     * <pre>
     *   first(text/html)        // returns text/html
     *   first(application/json) // returns application/json
     * </pre>
     *
     * @param candidates One ore more candidates media type. Required.
     * @return A first most relevant media type or an empty optional.
     */
    public Optional<MediaType> first(final List<MediaType> candidates) {
      return Optional.ofNullable(doFirst(candidates));
    }

    /**
     * Filter the accepted types and keep the most specifics media types.
     *
     * Given:
     *
     * <pre>
     *   text/html, application/xhtml; {@literal *}/{@literal *}
     * </pre>
     *
     * <pre>
     *   filter(text/html)       // returns text/html
     *   first(application/json) // returns application/json
     *   filter(text/html, application/json) // returns text/html and application/json
     * </pre>
     *
     * @param types A types to filter
     * @return Filtered types that matches the given types ordered from more specific to less
     *         specific.
     */
    public List<MediaType> filter(final List<MediaType> types) {
      checkArgument(types != null && types.size() > 0, "Media types are required");
      ImmutableList.Builder<MediaType> result = ImmutableList.builder();
      final List<MediaType> sortedTypes;
      if (types.size() == 1) {
        sortedTypes = ImmutableList.of(types.get(0));
      } else {
        sortedTypes = new LinkedList<>(types);
        Collections.sort(sortedTypes);
      }
      for (MediaType accept : acceptable) {
        for (MediaType candidate : sortedTypes) {
          if (accept.matches(candidate)) {
            result.add(candidate);
          }
        }
      }
      return result.build();
    }

    /**
     * Given:
     *
     * <pre>
     *   text/html, application/xhtml; {@literal *}/{@literal *}
     * </pre>
     *
     * <pre>
     *   first(text/html)        -> returns text/html
     *   first(application/json) -> returns application/json
     * </pre>
     *
     * @param candidates One ore more candidates media type. Required.
     * @return A first most relevant media type or an empty optional.
     */
    private MediaType doFirst(final List<MediaType> candidates) {
      List<MediaType> result = filter(candidates);
      return result.size() == 0 ? null : result.get(0);
    }
  }

  /**
   * Default parameters.
   */
  private static final Map<String, String> DEFAULT_PARAMS = ImmutableMap.of("q", "1");

  /**
   * A JSON media type.
   */
  public static final MediaType json = new MediaType("application", "json");

  private static final MediaType jsonLike = new MediaType("application", "*+json");

  /**
   * Any text media type.
   */
  public static final MediaType text = new MediaType("text", "*");

  /**
   * Text plain media type.
   */
  public static final MediaType plain = new MediaType("text", "plain");

  /**
   * Stylesheet media type.
   */
  public static final MediaType css = new MediaType("text", "css");

  /**
   * Javascript media types.
   */
  public static final MediaType js = new MediaType("application", "javascript");

  /**
   * HTML media type.
   */
  public static final MediaType html = new MediaType("text", "html");

  /**
   * The default binary media type.
   */
  public static final MediaType octetstream = new MediaType("application", "octet-stream");

  /**
   * Any media type.
   */
  public static final MediaType all = new MediaType("*", "*");

  /** Any media type. */
  public static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  /** Form multipart-data media type. */
  public static MediaType multipart = new MediaType("multipart", "form-data");

  /** Form url encoded. */
  public static MediaType form = new MediaType("application", "x-www-form-urlencoded");

  /** Xml media type. */
  public static MediaType xml = new MediaType("application", "xml");

  /** Xml like media type. */
  private static MediaType xmlLike = new MediaType("application", "*+xml");

  /**
   * Track the type of this media type.
   */
  private final String type;

  /**
   * Track the subtype of this media type.
   */
  private final String subtype;

  /**
   * Track the media type parameters.
   */
  private final Map<String, String> params;

  /**
   * True for wild-card types.
   */
  private final boolean wildcardType;

  /**
   * True for wild-card sub-types.
   */
  private final boolean wildcardSubtype;

  /**
   * Alias for most used types.
   */
  private static final Map<String, MediaType> alias = ImmutableMap.<String, MediaType> builder()
      .put("html", html)
      .put("json", json)
      .put("css", css)
      .put("js", js)
      .put("octetstream", octetstream)
      .put("form", form)
      .put("multipart", multipart)
      .put("xml", xml)
      .put("*", all)
      .build();

  static final Config types = ConfigFactory
      .parseResources("mime.properties")
      .withFallback(ConfigFactory.parseResources(MediaType.class, "mime.properties"));

  /**
   * Creates a new {@link MediaType}.
   *
   * @param type The primary type. Required.
   * @param subtype The secondary type. Required.
   * @param parameters The parameters. Required.
   */
  private MediaType(final String type, final String subtype, final Map<String, String> parameters) {
    this.type = requireNonNull(type, "A mime type is required.");
    this.subtype = requireNonNull(subtype, "A mime subtype is required.");
    this.params = ImmutableMap.copyOf(requireNonNull(parameters, "Parameters are required."));
    this.wildcardType = "*".equals(type);
    this.wildcardSubtype = "*".equals(subtype);
  }

  /**
   * Creates a new {@link MediaType}.
   *
   * @param type The primary type. Required.
   * @param subtype The secondary type. Required.
   */
  private MediaType(final String type, final String subtype) {
    this(type, subtype, DEFAULT_PARAMS);
  }

  /**
   * @return The quality of this media type. Default is: 1.
   */
  public float quality() {
    return Float.valueOf(params.get("q"));
  }

  /**
   * @return The primary media type.
   */
  public String type() {
    return type;
  }

  public Map<String, String> params() {
    return params;
  }

  /**
   * @return The secondary media type.
   */
  public String subtype() {
    return subtype;
  }

  /**
   * @return The qualified type {@link #type()}/{@link #subtype()}.
   */
  public String name() {
    return type + "/" + subtype;
  }

  /**
   * @return True, if this type is a well-known text type.
   */
  public boolean isText() {
    if (text.matches(this)) {
      return true;
    }
    if (js.matches(this)) {
      return true;
    }
    if (jsonLike.matches(this)) {
      return true;
    }
    if (xmlLike.matches(this)) {
      return true;
    }
    if (this.type.equals("application") && this.subtype.equals("hocon")) {
      return true;
    }

    return false;
  }

  @Override
  public int compareTo(final MediaType that) {
    requireNonNull(that, "A media type is required.");
    if (this == that) {
      return 0;
    }
    if (this.wildcardType && !that.wildcardType) {
      return 1;
    }

    if (that.wildcardType && !this.wildcardType) {
      return -1;
    }

    if (this.wildcardSubtype && !that.wildcardSubtype) {
      return 1;
    }

    if (that.wildcardSubtype && !this.wildcardSubtype) {
      return -1;
    }

    if (!this.type().equals(that.type())) {
      return 0;
    }

    int q = Float.compare(that.quality(), this.quality());
    if (q != 0) {
      return q;
    }
    // param size
    int paramsSize1 = this.params.size();
    int paramsSize2 = that.params.size();
    return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1));
  }

  /**
   * @param that A media type to compare to.
   * @return True, if the given media type matches the current one.
   */
  public boolean matches(final MediaType that) {
    requireNonNull(that, "A media type is required.");
    if (this == that || this.wildcardType || that.wildcardType) {
      // same or */*
      return true;
    }
    if (type.equals(that.type)) {
      if (subtype.equals(that.subtype) || this.wildcardSubtype || that.wildcardSubtype) {
        return true;
      }
      if (subtype.startsWith("*+")) {
        return that.subtype.endsWith(subtype.substring(2));
      }
      if (subtype.startsWith("*")) {
        return that.subtype.endsWith(subtype.substring(1));
      }
    }
    return false;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof MediaType) {
      MediaType that = (MediaType) obj;
      return type.equals(that.type) && subtype.equals(that.subtype) && params.equals(that.params);
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = prime + type.hashCode();
    result = prime * result + subtype.hashCode();
    result = prime * result + params.hashCode();
    return result;
  }

  @Override
  public final String toString() {
    return name();
  }

  /**
   * Convert a media type expressed as String into a {@link MediaType}.
   *
   * @param type A media type to parse.
   * @return An immutable {@link MediaType}.
   */
  public static MediaType valueOf(final String type) {
    requireNonNull(type, "A mediaType is required.");
    MediaType aliastype = alias.get(type.trim());
    if (aliastype != null) {
      return aliastype;
    }
    String[] parts = type.trim().split(";");
    String[] typeAndSubtype = parts[0].split("/");
    checkArgument(typeAndSubtype.length == 2, "Bad media type: %s", type);
    String stype = typeAndSubtype[0].trim();
    String subtype = typeAndSubtype[1].trim();
    checkArgument(!(stype.equals("*") && !subtype.equals("*")), "Bad media type: %s", type);
    Map<String, String> parameters = DEFAULT_PARAMS;
    if (parts.length > 1) {
      parameters = new LinkedHashMap<>(DEFAULT_PARAMS);
      for (int i = 1; i < parts.length; i++) {
        String[] parameter = parts[i].split("=");
        if (parameter.length > 1) {
          parameters.put(parameter[0].trim(), parameter[1].trim().toLowerCase());
        }
      }
    }
    return new MediaType(stype, subtype, parameters);
  }

  /**
   * Convert one or more media types expressed as String into a {@link MediaType}.
   *
   * @param types Media types to parse.
   * @return An list of immutable {@link MediaType}.
   */
  public static List<MediaType> valueOf(final String... types) {
    requireNonNull(types, "Types are required.");
    List<MediaType> result = new ArrayList<>();
    for (String type : types) {
      result.add(valueOf(type));
    }
    return result;
  }

  /**
   * Convert a string separated by comma into one or more {@link MediaType}.
   *
   * @param value The string separated by commas.
   * @return One ore more {@link MediaType}.
   */
  public static List<MediaType> parse(final String value) {
    return valueOf(value.split(","));
  }

  /**
   * Produces a matcher for the given media type.
   *
   * @param acceptable The acceptable/target media type.
   * @return A media type matcher.
   */
  public static Matcher matcher(final MediaType acceptable) {
    return matcher(ImmutableList.of(acceptable));
  }

  /**
   * Produces a matcher for the given media types.
   *
   * @param acceptable The acceptable/target media types.
   * @return A media type matcher.
   */
  public static Matcher matcher(final List<MediaType> acceptable) {
    requireNonNull(acceptable, "Acceptables media types are required.");
    return new Matcher(acceptable);
  }

  /**
   * Get a {@link MediaType} for a file.
   *
   * @param file A candidate file.
   * @return A {@link MediaType} or {@link MediaType#octetstream} for unknown file extensions.
   */
  public static Optional<MediaType> byFile(final File file) {
    requireNonNull(file, "A file is required.");
    return byPath(file.getName());
  }

  /**
   * Get a {@link MediaType} for a file path.
   *
   * @param path A candidate file path.
   * @return A {@link MediaType} or empty optional for unknown file extensions.
   */
  public static Optional<MediaType> byPath(final Path path) {
    requireNonNull(path, "A path is required.");
    return byPath(path.toString());
  }

  /**
   * Get a {@link MediaType} for a file path.
   *
   * @param path A candidate file path: like <code>myfile.js</code> or <code>/js/myfile.js</code>.
   * @return A {@link MediaType} or empty optional for unknown file extensions.
   */
  public static Optional<MediaType> byPath(final String path) {
    requireNonNull(path, "A path is required.");
    int idx = path.lastIndexOf('.');
    if (idx != -1) {
      String ext = path.substring(idx + 1);
      return byExtension(ext);
    }
    return Optional.empty();
  }

  /**
   * Get a {@link MediaType} for a file extension.
   *
   * @param ext A file extension, like <code>js</code> or <code>css</code>.
   * @return A {@link MediaType} or empty optional for unknown file extensions.
   */
  public static Optional<MediaType> byExtension(final String ext) {
    requireNonNull(ext, "An ext is required.");
    String key = "mime." + ext;
    if (types.hasPath(key)) {
      return Optional.of(MediaType.valueOf(types.getString("mime." + ext)));
    }
    return Optional.empty();
  }

}