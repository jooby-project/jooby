/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.openapi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;

public class MixinHook {

  @JsonPropertyOrder({
    "content",
    "numberOfElements",
    "totalElements",
    "totalPages",
    "pageRequest",
    "nextPageRequest",
    "previousPageRequest"
  })
  public abstract static class PageMixin<T> implements Page<T> {
    @JsonProperty("content")
    @Override
    public abstract List<T> content();

    @JsonProperty("numberOfElements")
    @Override
    public abstract int numberOfElements();

    @Override
    @JsonProperty("pageRequest")
    public abstract PageRequest pageRequest();

    @Override
    @JsonProperty("nextPageRequest")
    public abstract PageRequest nextPageRequest();

    @Override
    @JsonProperty("previousPageRequest")
    public abstract PageRequest previousPageRequest();

    @Override
    @JsonProperty("totalElements")
    public abstract long totalElements();

    @Override
    @JsonProperty("totalPages")
    public abstract long totalPages();
  }

  @JsonPropertyOrder({"page", "size"})
  public abstract static class PageRequestMixin implements PageRequest {
    @JsonProperty("page")
    @Override
    @ApiModelProperty("The page to be returned")
    public abstract long page();

    @JsonProperty("size")
    @Override
    @ApiModelProperty("The requested size of each page")
    public abstract int size();
  }

  public static void mixin(ObjectMapper mapper) {
    mapper.addMixIn(jakarta.data.page.Page.class, PageMixin.class);
    mapper.addMixIn(jakarta.data.page.PageRequest.class, PageRequestMixin.class);
  }
}
