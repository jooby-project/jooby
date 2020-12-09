/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class HttpRawPostRequestDecoder implements InterfaceHttpPostRequestDecoder {

  private HttpRequest request;
  private HttpDataFactory factory;

  private HttpData data;

  public HttpRawPostRequestDecoder(HttpDataFactory factory, HttpRequest request) {
    this.factory = factory;
    this.request = request;
    this.data = factory.createAttribute(request, "body");
  }

  @Override public boolean isMultipart() {
    return false;
  }

  @Override public void setDiscardThreshold(int discardThreshold) {
  }

  @Override public int getDiscardThreshold() {
    return 0;
  }

  @Override public List<InterfaceHttpData> getBodyHttpDatas() {
    return data == null ? Collections.emptyList() : Collections.singletonList(data);
  }

  @Override public List<InterfaceHttpData> getBodyHttpDatas(String name) {
    return getBodyHttpDatas();
  }

  @Override public InterfaceHttpData getBodyHttpData(String name) {
    return data;
  }

  @Override public InterfaceHttpPostRequestDecoder offer(HttpContent content) {
    try {
      data.addContent(content.content().copy(), content instanceof LastHttpContent);
      return this;
    } catch (IOException x) {
      throw new HttpPostRequestDecoder.ErrorDataDecoderException(x);
    }
  }

  @Override public boolean hasNext() {
    return data != null;
  }

  @Override public InterfaceHttpData next() {
    return data;
  }

  @Override public InterfaceHttpData currentPartialHttpData() {
    return data;
  }

  @Override public void destroy() {
    cleanFiles();
    removeHttpDataFromClean(data);
    data.delete();
  }

  @Override public void cleanFiles() {
    factory.cleanRequestHttpData(request);
  }

  @Override public void removeHttpDataFromClean(InterfaceHttpData data) {
    factory.removeHttpDataFromClean(request, data);
  }
}
