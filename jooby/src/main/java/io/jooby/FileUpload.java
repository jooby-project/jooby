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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface FileUpload extends Value {
  String getFileName();

  String getContentType();

  @Override default Value get(@Nonnull int index) {
    return index == 0 ? this : get(Integer.toString(index));
  }

  @Override default Value get(@Nonnull String name) {
    return new Missing(name);
  }

  @Override default int size() {
    return 1;
  }

  @Override default @Nonnull String value() {
    return value(StandardCharsets.UTF_8);
  }

  default @Nonnull String value(Charset charset) {
    return new String(bytes(), charset);
  }

  @Override default Map<String, List<String>> toMultimap() {
    Map<String, List<String>> result = new HashMap<>(1);
    result.put(name(), Collections.singletonList(getFileName()));
    return result;
  }

  InputStream stream();

  byte[] bytes();

  Path path();

  long getFileSize();

  @Override default FileUpload fileUpload() {
    return this;
  }

  void destroy();
}
