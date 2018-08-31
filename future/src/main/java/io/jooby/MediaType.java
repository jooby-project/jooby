package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaType {

  public static final String JSON = "application/json";

  public static final String TEXT = "text/plain";

  public static final String HTML = "text/html";

  public static final String JS = "application/javascript";

  public static final String CSS = "text/css";

  public static final String OCTET_STREAM = "application/octet-stream";

  public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

  public static final String MULTIPART_FORMDATA = "multipart/form-data";

  public static final String ALL = "*/*";

  public static final MediaType json = new MediaType(JSON);

  public static final MediaType text = new MediaType(TEXT);

  public static final MediaType html = new MediaType(HTML);

  public static final MediaType js = new MediaType(JS);

  public static final MediaType css = new MediaType(CSS);

  public static final MediaType octetStream = new MediaType(OCTET_STREAM);

  public static final MediaType formUrlencoded = new MediaType(FORM_URLENCODED);

  public static final MediaType multipartFormdata = new MediaType(MULTIPART_FORMDATA);

  public static final MediaType all = new MediaType(ALL);

  private final String value;

  private final int subtypeStart;

  private final int subtypeEnd;

  private MediaType(@Nonnull String value) {
    // Old browsers send `*` which means `*/*`
    if (value.equals("*")) {
      this.value = "*/*";
      this.subtypeStart = 1;
      this.subtypeEnd = this.value.length();
    } else {
      this.value = value;
      this.subtypeStart = value.indexOf('/');
      if (subtypeStart < 0) {
        throw new IllegalArgumentException("Invalid media type: " + value);
      }
      int subtypeEnd = value.indexOf(';');
      this.subtypeEnd = subtypeEnd < 0 ? value.length() : subtypeEnd;
    }
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof MediaType) {
      MediaType that = (MediaType) obj;
      return type().equals(that.type()) && subtype().equals(that.subtype());
    }
    return false;
  }

  @Override public int hashCode() {
    return value.hashCode();
  }

  @Nullable public String param(@Nonnull String name) {
    int paramStart = subtypeEnd + 1;
    for (int i = subtypeEnd; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch == '=') {
        String pname = value.substring(paramStart, i).trim();
        int paramValueEnd = value.indexOf(';', i);
        if (paramValueEnd < 0) {
          paramValueEnd = value.length();
        }
        if (pname.equals(name)) {
          return value.substring(i + 1, paramValueEnd).trim();
        }
        paramStart = paramValueEnd + 1;
        i = paramStart;
      }
    }
    return null;
  }

  @Nonnull public String value() {
    return value;
  }

  @Nonnull public float quality() {
    String q = param("q");
    return q == null ? 1f : Float.parseFloat(q);
  }

  @Nonnull public Charset charset() {
    String charset = param("charset");
    return charset == null ? StandardCharsets.UTF_8 : Charset.forName(charset);
  }

  @Nonnull public String type() {
    return value.substring(0, subtypeStart).trim();
  }

  @Nonnull public String subtype() {
    return value.substring(subtypeStart + 1, subtypeEnd).trim();
  }

  @Nonnull public static MediaType valueOf(@Nonnull String value) {
    if (value == null || value.length() == 0) {
      return all;
    }
    return new MediaType(value);
  }

  @Nonnull public static List<MediaType> parse(@Nullable String value) {
    if (value == null || value.length() == 0) {
      return Collections.emptyList();
    }
    List<MediaType> result = new ArrayList<>(3);
    int typeStart = 0;
    int len = value.length();
    for(int i = 0; i < len; i++) {
      char ch = value.charAt(i);
      if (ch == ',') {
        result.add(valueOf(value.substring(typeStart, i).trim()));
        typeStart = i + 1;
      }
    }
    if (typeStart < len) {
      result.add(valueOf(value.substring(typeStart, len).trim()));
    }
    return result;
  }

  @Override public String toString() {
    return value;
  }
}
