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

import java.net.URL;
import java.util.Set;

public class EbeanEnhancer {

  protected final String transformerClass;

  protected final String agentLoaderClass;

  EbeanEnhancer(final String transformerClass, final String agentLoaderClass) {
    this.transformerClass = transformerClass;
    this.agentLoaderClass = agentLoaderClass;
  }

  public void run(final Set<String> packages) {
    // NOOP;
  }

  public static EbeanEnhancer newEnhancer() {
    return newEnhancer("/io/ebean/enhance/Transformer.class",
        "/org/avaje/agentloader/AgentLoader.class");
  }

  public static EbeanEnhancer newEnhancer(final String transformerClass,
      final String agentLoaderClass) {
    String[] names = {transformerClass, agentLoaderClass };
    for (String name : names) {
      URL resource = EbeanEnhancer.class.getResource(name);
      if (resource == null) {
        return new EbeanEnhancer(transformerClass, agentLoaderClass);
      }
    }
    return new EbeanAgentEnhancer(transformerClass, agentLoaderClass);
  }
}
