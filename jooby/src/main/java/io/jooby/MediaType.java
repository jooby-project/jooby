/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  public static final MediaType json = new MediaType(JSON, "UTF-8");

  public static final MediaType text = new MediaType(TEXT, "UTF-8");

  public static final MediaType html = new MediaType(HTML, "UTF-8");

  public static final MediaType js = new MediaType(JS, "UTF-8");

  public static final MediaType css = new MediaType(CSS, "UTF-8");

  public static final MediaType octetStream = new MediaType(OCTET_STREAM, null);

  public static final MediaType formUrlencoded = new MediaType(FORM_URLENCODED, "UTF-8");

  public static final MediaType multipartFormdata = new MediaType(MULTIPART_FORMDATA, null);

  public static final MediaType all = new MediaType(ALL, null);

  private final String value;

  private final String charset;

  private final int subtypeStart;

  private final int subtypeEnd;

  private MediaType(@Nonnull String value, String charset) {
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
    this.charset = charset;
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
    return value.substring(0, subtypeEnd);
  }

  @Nonnull public float quality() {
    String q = param("q");
    return q == null ? 1f : Float.parseFloat(q);
  }

  public boolean isTextual() {
    String type = type();
    if (type.equals("text")) {
      return true;
    }
    if (type.equals("application")) {
      String subtype = subtype();
      return subtype.equals("json") || subtype.equals("javascript");
    }
    return false;
  }

  @Nullable public String charset() {
    String charset = _charset(this.charset);
    if (charset == null && isTextual()) {
      return "UTF-8";
    }
    return charset;
  }

  private String _charset(String charset) {
    String charsetName = param("charset");
    return charsetName == null ? charset : charsetName;
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
    return new MediaType(value, null);
  }

  @Nonnull public static List<MediaType> parse(@Nullable String value) {
    if (value == null || value.length() == 0) {
      return Collections.emptyList();
    }
    List<MediaType> result = new ArrayList<>(3);
    int typeStart = 0;
    int len = value.length();
    for (int i = 0; i < len; i++) {
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
