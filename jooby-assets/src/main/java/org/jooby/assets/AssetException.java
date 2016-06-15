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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

/**
 * Carry information about a asset processor failure. It might have multiple {@link AssetProblem}
 * and the id of the processor who fail to process the input.
 *
 * @author edgar
 */
@SuppressWarnings("serial")
public class AssetException extends RuntimeException implements Supplier<String> {

  private List<AssetProblem> problems;

  private String id;

  /**
   * Creates a new {@link AssetException}.
   *
   * @param id Processor id.
   * @param problem Asset problem.
   */
  public AssetException(final String id, final AssetProblem problem) {
    this(id, ImmutableList.of(problem));
  }

  /**
   * Creates a new {@link AssetException}.
   *
   * @param id Processor id.
   * @param problems Asset problems.
   */
  public AssetException(final String id, final List<AssetProblem> problems) {
    super(requireNonNull(problems, "The problems is required.").stream().map(AssetProblem::toString)
        .collect(Collectors.joining("\n", "\t", "")));
    this.id = id;
    this.problems = problems;
  }

  /**
   * @return Message of the first problem (useful for better error display on whoops).
   */
  @Override
  public String get() {
    AssetProblem problem = problems.get(0);
    return problem.getMessage();
  }

  /**
   * @return processor ID.
   */
  public String getId() {
    return id;
  }

  /**
   * @return List of asset problems.
   */
  public List<AssetProblem> getProblems() {
    return problems;
  }

}
