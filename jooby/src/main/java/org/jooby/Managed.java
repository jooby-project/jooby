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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Instances of {@link Managed} are started and/or stopped by Jooby.
 *
 * Please note, that every single object can implement this interface but for none singleton
 * objects, the object will be keep on memory waiting for application shutdown. That's why it is
 * strongly recommend to ONLY use this on singleton objects.
 *
 * {@link PostConstruct} and {@link PreDestroy} annotation are supported too. {@link PostConstruct}
 * works for singletons and none singletons (no restriction). {@link PreDestroy} works for
 * singletons and none singletons. But again, it is strongly recommended to use it on singleton
 * objects.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface Managed {

  /**
   * Start callback, useful to initialize an expensive service.
   *
   * @throws Throwable If something goes wrong.
   */
  void start() throws Throwable;

  /**
   * Stop callback, useful for cleanup and free resources. ONLY for singleton objects.
   *
   * @throws Throwable If something goes wrong.
   */
  void stop() throws Throwable;

}
