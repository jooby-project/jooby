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
package org.jooby.internal.hazelcast;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HcastManaged implements Provider<HazelcastInstance> {

  private Config config;

  private HazelcastInstance hazelcast;

  @Inject
  public HcastManaged(final Config config) {
    this.config = config;
  }

  @PostConstruct
  public void start() throws Exception {
    hazelcast = Hazelcast.newHazelcastInstance(config);
  }

  @Override
  public HazelcastInstance get() {
    return hazelcast;
  }

  @PreDestroy
  public void stop() throws Exception {
    if (hazelcast != null) {
      hazelcast.shutdown();
      hazelcast = null;
    }
  }

}
