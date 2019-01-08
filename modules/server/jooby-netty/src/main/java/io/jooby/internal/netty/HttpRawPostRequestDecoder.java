/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class HttpRawPostRequestDecoder implements InterfaceHttpPostRequestDecoder {

  private HttpData data;

  public HttpRawPostRequestDecoder(HttpData data) {
    this.data = data;
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
      data.addContent(content.content(), content instanceof LastHttpContent);
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
    data.delete();
  }

  @Override public void cleanFiles() {
  }

  @Override public void removeHttpDataFromClean(InterfaceHttpData data) {
  }
}
