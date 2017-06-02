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
package org.jooby.internal.mongodb;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jooby.mongodb.JongoFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class JongoFactoryImpl implements JongoFactory, Provider<Jongo> {

  private MongoClient client;

  private String db;

  private Mapper mapper;

  @Inject
  public JongoFactoryImpl(final MongoClientURI uri, final MongoClient client, final Mapper mapper) {
    this.db = requireNonNull(uri, "MongoClientURI is required.").getDatabase();
    this.client = requireNonNull(client, "MongoClient is required.");
    this.mapper = requireNonNull(mapper, "Mapper is required.");
  }

  @Override
  public Jongo get() {
    return get(db);
  }

  @SuppressWarnings("deprecation")
  @Override
  public Jongo get(final String db) {
    return new Jongo(client.getDB(db), mapper);
  }

}
