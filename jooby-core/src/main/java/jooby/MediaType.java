package jooby;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;

public class MediaType implements Comparable<MediaType> {

  public static class Matcher {

    private Iterable<MediaType> acceptable;

    Matcher(final Iterable<MediaType> acceptable) {
      this.acceptable = acceptable;
    }

    public boolean matches(final MediaType candidate) {
      return doFirst(ImmutableSortedSet.of(candidate)) != null;
    }

    public boolean matches(final Iterable<MediaType> candidates) {
      return doFirst(candidates) != null;
    }

    public Optional<MediaType> first(final MediaType candidate) {
      return first(ImmutableList.of(candidate));
    }

    public Optional<MediaType> first(final Iterable<MediaType> candidates) {
      return Optional.ofNullable(doFirst(candidates));
    }

    public List<MediaType> filter(final Iterable<MediaType> candidates) {
      List<MediaType> result = new ArrayList<>();
      for (MediaType accept : acceptable) {
        for (MediaType candidate : candidates) {
          if (accept.matches(candidate)) {
            // when */* choose the more specific
            result.add(candidate.wildcardType ? accept : candidate);
          }
        }
      }
      return result;
    }

    private MediaType doFirst(final Iterable<MediaType> candidates) {
      SortedSet<MediaType> result = new TreeSet<>();
      for (MediaType accept : acceptable) {
        for (MediaType candidate : candidates) {
          if (accept.matches(candidate)) {
            result.add(candidate);
          }
        }
      }
      return result.size() == 0 ? null : result.first();
    }
  }

  private static final Map<String, String> DEFAULT_PARAMS = ImmutableMap.of("q", "1");

  public static final MediaType json = new MediaType("application", "json");

  public static final List<MediaType> JSON = ImmutableList.of(json);

  public static final MediaType plain = new MediaType("text", "plain");

  public static final MediaType html = new MediaType("text", "html");

  public static final List<MediaType> HTML = ImmutableList.of(html);

  public static final MediaType all = new MediaType("*", "*");

  public static final List<MediaType> ALL = ImmutableList.of(all);

  private final String type;

  private final String subtype;

  private final Map<String, String> params;

  private boolean wildcardType;

  private boolean wildcardSubtype;

  private MediaType(final String type, final String subtype, final Map<String, String> parameters) {
    this.type = requireNonNull(type, "A mime type is required.");
    this.subtype = requireNonNull(subtype, "A mime subtype is required.");
    this.params = requireNonNull(parameters, "The parameters is required.");
    this.wildcardType = "*".equals(type);
    this.wildcardSubtype = "*".equals(subtype);
  }

  private MediaType(final String type, final String subtype) {
    this(type, subtype, DEFAULT_PARAMS);
  }

  public float quality() {
    return Float.valueOf(params.get("q"));
  }

  public String parameter(final String name) {
    return params.get(name);
  }

  public String type() {
    return type;
  }

  public String subtype() {
    return subtype;
  }

  public String name() {
    return type + "/" + subtype;
  }

  @Override
  public int compareTo(final MediaType that) {
    if (this == that) {
      return 0;
    }
    if (this.type.equals(that.type)) {
      if (this.subtype.equals(that.subtype)) {
        return quality(this, that);
      } else {
        // subtype differ
        if (this.wildcardSubtype) {
          return 1;
        }
        if (that.wildcardSubtype) {
          return -1;
        }
        return quality(this, that);
      }
    } else if (this.wildcardType) {
      return 1;
    } else {
      return -1;
    }
  }

  public boolean matches(final MediaType that) {
    if (this == that || this.wildcardType || that.wildcardType) {
      // same or */*
      return true;
    }
    if (type.equals(that.type)) {
      if (subtype.equals(that.subtype) || this.wildcardSubtype) {
        return true;
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

  public static MediaType valueOf(final String mediaType) {
    requireNonNull(mediaType, "A mediaType is required.");
    String[] parts = mediaType.split(";");
    checkArgument(parts.length > 0, "Bad media type: %s", mediaType);
    String[] typeAndSubtype = (parts[0].equals("*") ? "*/*" : parts[0]).split("/");
    checkArgument(typeAndSubtype.length == 2, "Bad media type: %s", mediaType);
    String type = typeAndSubtype[0].trim();
    String subtype = typeAndSubtype[1].trim();
    checkArgument(!(type.equals("*") && !subtype.equals("*")), "Bad media type: %s", mediaType);
    Map<String, String> parameters = DEFAULT_PARAMS;
    if (parts.length > 1) {
      parameters = new LinkedHashMap<>(DEFAULT_PARAMS);
      for (int i = 1; i < parts.length; i++) {
        String[] parameter = parts[i].split("=");
        if (parameter.length > 1) {
          parameters.put(parameter[0].trim(), parameter[1].trim());
        }
      }
    }
    return new MediaType(type, subtype, parameters);
  }

  public static List<MediaType> valueOf(final String... mediaTypes) {
    requireNonNull(mediaTypes, "A mediaType is required.");
    List<MediaType> result = new ArrayList<>();
    for (String mediaType : mediaTypes) {
      result.add(valueOf(mediaType));
    }
    return result;
  }

  public static List<MediaType> parse(final String value) {
    return valueOf(value.split(","));
  }

  public static Matcher matcher(final MediaType acceptable) {
    return matcher(ImmutableList.of(acceptable));
  }

  public static Matcher matcher(final Iterable<MediaType> acceptable) {
    return new Matcher(acceptable);
  }

  private static int quality(final MediaType it, final MediaType that) {
    float q = that.quality() - it.quality();
    if (q == 0) {
      int p = that.params.size() - it.params.size();
      if (p == 0) {
        int alphanumeric = it.subtype.compareTo(that.subtype);
        if (alphanumeric == 0) {
          return that.params.toString().compareTo(it.params.toString());
        }
        return alphanumeric;
      }
      return p;
    }
    return q < 0 ? -1 : 1;
  }

}
