package org.jooby.apitool.raml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

public class RamlResponse {
  private String description;
  private Map<String, RamlType> mediaType;

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  @JsonAnyGetter
  Map<String, Object> attributes() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    if (mediaType != null) {
      if (mediaType.size() == 1) {
        Map.Entry<String, RamlType> e = mediaType.entrySet().iterator().next();
        if (e.getKey() == null) {
          attributes.put("body", e.getValue().getRef());
        }
      } else {
        Map<String, Object> body = new LinkedHashMap<>();
        mediaType.forEach((type, bodyType) -> body.put(type, bodyType.getRef()));
        attributes.put("body", body);
      }
    }
    return attributes.size() == 0 ? null : attributes;
  }

  @JsonIgnore
  public Map<String, RamlType> getMediaType() {
    return mediaType;
  }

  public void setMediaType(final String mediaType, RamlType body) {
    if (this.mediaType == null) {
      this.mediaType = new LinkedHashMap<>();
    }
    this.mediaType.put(mediaType, body);
  }
}
