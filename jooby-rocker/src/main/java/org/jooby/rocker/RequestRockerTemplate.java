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
package org.jooby.rocker;

import static com.fizzed.rocker.RockerUtils.requireTemplateClass;

import java.util.Map;

import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.RockerTemplate;
import com.fizzed.rocker.runtime.DefaultRockerTemplate;

public abstract class RequestRockerTemplate extends DefaultRockerTemplate {

  public Map<String, Object> locals;

  public RequestRockerTemplate(final RockerModel model) {
    super(model);
  }

  @Override
  protected void __associate(final RockerTemplate context) {
    this.locals = requireTemplateClass(context, RequestRockerTemplate.class).locals;
  }

}
