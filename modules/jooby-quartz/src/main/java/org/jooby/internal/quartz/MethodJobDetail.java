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
package org.jooby.internal.quartz;

import java.lang.reflect.Method;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.impl.JobDetailImpl;

@SuppressWarnings("serial")
public class MethodJobDetail extends JobDetailImpl {

  private Class<?> owner;

  public MethodJobDetail(final Method method) {
    this.owner = method.getDeclaringClass();
  }

  @Override
  public boolean isConcurrentExectionDisallowed() {
    return owner.getAnnotation(DisallowConcurrentExecution.class) != null;
  }

  @Override
  public boolean isPersistJobDataAfterExecution() {
    return owner.getAnnotation(PersistJobDataAfterExecution.class) != null;
  }
}
