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

import java.lang.reflect.Field;

import org.jooby.mongodb.GeneratedValue;
import org.jooby.mongodb.IdGen;
import org.mongodb.morphia.AbstractEntityInterceptor;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.DBObject;

public class AutoIncID extends AbstractEntityInterceptor {

  private Datastore db;

  private IdGen gen;

  public AutoIncID(final Datastore db, final IdGen gen) {
    this.db = db;
    this.gen = gen;
  }

  @Override
  public void prePersist(final Object entity, final DBObject dbObj, final Mapper mapper) {
    MappedClass mclass = mapper.getMappedClass(entity);
    Field id = mclass.getIdField();
    if (id.getAnnotation(GeneratedValue.class) != null) {
      try {
        id.setAccessible(true);
        final String collName = gen.value(mclass.getClazz());
        final Query<StoredId> q = db.find(StoredId.class, "_id", collName);
        final UpdateOperations<StoredId> uOps = db.createUpdateOperations(StoredId.class)
            .inc("value");
        StoredId newId = db.findAndModify(q, uOps);
        if (newId == null) {
          newId = new StoredId(collName);
          db.save(newId);
        }
        id.set(entity, newId.value);
      } catch (Exception ex) {
        throw new IllegalStateException("Can't generate ID on " + mclass, ex);
      }
    }
  }

}
