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
package org.jooby.internal.ebean;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

import org.avaje.agentloader.AgentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EbeanAgentEnhancer extends EbeanEnhancer {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  EbeanAgentEnhancer(final String transformerClass, final String agentLoaderClass) {
    super(transformerClass, agentLoaderClass);
  }

  @Override
  public void run(final Set<String> packages) {
    try {
      String params = "debug=0;packages=" + packages.stream()
          .map(pkg -> pkg + ".**")
          .collect(Collectors.joining(","));

      URL resource = getClass().getResource(transformerClass);
      String location = resource.getPath();
      int idx = location.indexOf(".jar!");
      if (idx > 0) {
        location = location.substring(0, idx + ".jar".length());
      }
      File jarLocation = location.startsWith("file:")
          ? new File(URI.create(location))
          : new File(location);

      AgentLoader.loadAgent(jarLocation.getAbsolutePath(), params);
    } catch (Exception ex) {
      log.trace("agent already loaded", ex);
    }
  }
}
