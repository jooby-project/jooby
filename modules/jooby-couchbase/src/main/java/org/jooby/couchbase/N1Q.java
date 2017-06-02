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
package org.jooby.couchbase;

import static com.couchbase.client.java.query.dsl.Expression.i;
import static com.couchbase.client.java.query.dsl.Expression.path;
import static com.couchbase.client.java.query.dsl.Expression.s;

import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.Select;
import com.couchbase.client.java.query.dsl.path.GroupByPath;

/**
 * Utility methods around queries and ID generation.
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public final class N1Q {

  /** ID separator . */
  private static final String ID_SEP = "::";

  /** Default bucket . */
  static final String COUCHBASE_DEFBUCKET = "couchbase.defbucket";

  /** Meta attribute for storing document type. */
  public static final String CLASS = "_class";

  /**
   * Creates a new {@link N1qlQuery} for the given class, this method build a query like:
   *
   * <pre>
   * select b.* from bucket b where b._class = 'model.Beer'
   * </pre>
   *
   * @param entityClass Entity class.
   * @return A query.
   */
  public static GroupByPath from(final Class<?> entityClass) {
    return from(System.getProperty(COUCHBASE_DEFBUCKET, "default"), entityClass);
  }

  /**
   * Creates a new {@link N1qlQuery} for the given class, this method build a query like:
   *
   * <pre>
   * select b.* from bucket b where b._class = 'model.Beer'
   * </pre>
   *
   * @param bucket Bucket name.
   * @param entityClass Entity class.
   * @return A query.
   */
  public static GroupByPath from(final String bucket, final Class<?> entityClass) {
    String alias = String.valueOf(entityClass.getSimpleName().charAt(0));
    return Select.select(alias + ".*")
        .from(i(bucket) + " " + alias)
        .where(path(alias, CLASS).eq(s(entityClass.getName())));
  }

  /**
   * Qualify and ID to construct and unique ID. Given: model.Beer and "ipa" this method returns:
   * <code>model.Beer::ipa</code>.
   *
   * @param entityClass Entity class.
   * @param id Entity id.
   * @return Qualified ID.
   */
  public static String qualifyId(final Class<?> entityClass, final Object id) {
    return qualifyId(entityClass.getName(), id);
  }

  /**
   * Qualify and ID to construct and unique ID. Given: "session" and "ipa" this method returns:
   * <code>session::ipa</code>.
   *
   * @param prefix Prefix class.
   * @param id Object id.
   * @return Qualified ID.
   */
  public static String qualifyId(final String prefix, final Object id) {
    return prefix + ID_SEP + id;
  }
}
