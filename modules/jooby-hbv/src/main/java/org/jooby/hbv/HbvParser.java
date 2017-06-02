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
package org.jooby.hbv;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.function.Predicate;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.jooby.Parser;

import com.google.inject.TypeLiteral;

class HbvParser implements Parser {

  private Predicate<TypeLiteral<?>> predicate;

  public HbvParser(final Predicate<TypeLiteral<?>> predicate) {
    this.predicate = requireNonNull(predicate, "Predicate is required.");
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
    Object value = ctx.next();

    if (predicate.test(type)) {
      Validator validator = ctx.require(Validator.class);

      Set<ConstraintViolation<Object>> violations = validator.validate(value);
      if (violations.size() > 0) {
        throw new ConstraintViolationException(violations);
      }
    }

    return value;
  }

  @Override
  public String toString() {
    return "hbv";
  }
}
