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
package org.jooby.spi;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * File upload from multipart/form-data post.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface NativeUpload extends Closeable {

  /**
   * @return File name.
   */
  String name();

  /**
   * File header, like <code>Content-Type</code>, <code>Content-Transfer-Encoding</code>, etc.
   *
   * @param name Header's name.
   * @return A header value or empty optional.
   */
  Optional<String> header(String name);

  /**
   * Get all the file headers for the given name.
   *
   * @param name A header's name.
   * @return All available values or and empty list.
   */
  List<String> headers(String name);

  /**
   * Get the actual file link/reference and do something with it.
   *
   * @return A file from local file system.
   * @throws IOException If file failed to read/write from local file system.
   */
  File file() throws IOException;

}
