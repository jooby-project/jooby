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
package org.jooby.elasticsearch;

import java.util.Arrays;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.jooby.Env;
import org.jooby.Jooby;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <p>
 * Full text search and analytics via <a href="https://github.com/elastic/elasticsearch">Elastic
 * Search</a>.
 * </p>
 * Provides a RESTFul client.
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Elasticsearch("localhost:9200"));
 * }
 * </pre>

 * <h1>client API</h1>
 * <p>
 * The module exposes a {@link RestClient} instance
 * </p>
 *
 * <pre>{@code
 *
 * put("/:id", req -> {
 *   // index a document
 *   RestClient client = req.require(RestClient.class);
 *   StringEntity data = new StringEntity("{\"foo\":\"bar\"}");
 *   return client.performRequest("PUT", "/twitter/tweet/" + req.param("id").value(), Collections.emptyMap(), data)
 *     .getEntity().getContent();
 * });
 *
 * }</pre>
 *
 * @author edgar
 * @since 0.6.0
 */
public class Elasticsearch implements Jooby.Module {

  private final String[] hosts;

  public Elasticsearch() {
    this("localhost:9200");
  }

  public Elasticsearch(final String ... hosts) {
    this.hosts = hosts;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    HttpHost[] httpHosts = Arrays.stream(hosts).map(HttpHost::create).toArray(HttpHost[]::new);
    RestClient restClient = RestClient.builder(httpHosts).build();
    binder.bind(RestClient.class).toInstance(restClient);

    env.onStop(restClient::close);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Elasticsearch.class, "es.conf");
  }
}
