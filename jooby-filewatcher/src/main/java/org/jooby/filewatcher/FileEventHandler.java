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
package org.jooby.filewatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * Callback call when a file or folder has been created, modified or deleted.
 *
 * @author edgar
 * @since 1.1.0
 */
public interface FileEventHandler {

  /**
   * File handler callback.
   *
   * @param kind Event type.
   * @param path Path created, modified or deleted.
   * @throws IOException If something goes wrong.
   */
  void handle(WatchEvent.Kind<Path> kind, Path path) throws IOException;

}
