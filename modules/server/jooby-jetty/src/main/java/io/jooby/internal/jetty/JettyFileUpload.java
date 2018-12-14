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
package io.jooby.internal.jetty;

import io.jooby.FileUpload;
import io.jooby.Value;
import org.eclipse.jetty.http.MultiPartFormInputStream;
import io.jooby.Throwing;

import java.io.IOException;
import java.nio.file.Path;

public class JettyFileUpload extends Value.Simple implements FileUpload {
  private final MultiPartFormInputStream.MultiPart upload;

  public JettyFileUpload(String name, MultiPartFormInputStream.MultiPart upload) {
    super(name, upload.getSubmittedFileName());
    this.upload = upload;
  }

  @Override public String contentType() {
    return upload.getContentType();
  }

  @Override public Path path() {
    try {
      if (upload.getFile() == null) {
        upload.write("jetty" + System.currentTimeMillis() + ".tmp");
      }
      return upload.getFile().toPath();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Override public long filesize() {
    return upload.getSize();
  }

  @Override public void destroy() {
    try {
      upload.cleanUp();
      upload.delete();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
