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
package org.jooby;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * File upload from a browser on {@link MediaType#multipart} request.
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Upload extends Closeable {

  /**
   * @return File's name.
   */
  String name();

  /**
   * @return File media type.
   */
  MediaType type();

  /**
   * Upload header, like content-type, charset, etc...
   *
   * @param name Header's name.
   * @return A header value.
   */
  Mutant header(String name);

  /**
   * Get this upload as temporary file.
   *
   * @return A temp file.
   * @throws IOException If file doesn't exist.
   */
  File file() throws IOException;

}
