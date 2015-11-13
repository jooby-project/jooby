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
package org.jooby.assets;

import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.collect.ImmutableList;

@SuppressWarnings("serial")
public class AssetException extends RuntimeException {

  private List<AssetProblem> problems;

  private String id;

  public AssetException(final String id, final AssetProblem problem) {
    this(id, ImmutableList.of(problem));
  }

  public AssetException(final String id, final List<AssetProblem> problems) {
    super(requireNonNull(problems, "The problems is required.").toString());
    this.id = id;
    this.problems = problems;
  }

  /**
   * @return processor ID.
   */
  public String getId() {
    return id;
  }

  public List<AssetProblem> getProblems() {
    return problems;
  }

}
