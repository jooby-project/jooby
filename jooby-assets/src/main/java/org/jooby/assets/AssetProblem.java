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

/**
 * A problem found while processing an asset. Attributes {@link #getFilename()} and
 * {@link #getMessage()} are always present, while {@link #getLine()} and {@link #getColumn()} might
 * have <code>-1</code> when missing.
 *
 * @author edgar
 * @since 0.11.0
 */
public class AssetProblem {

  private String filename;

  private int line;

  private int column;

  private String message;

  private String evidence;

  public AssetProblem(final String filename, final int line, final int column,
      final String message, final String evidence) {
    this.filename = filename;
    this.line = line;
    this.column = column;
    this.message = message;
    this.evidence = evidence;
  }

  /**
   * @return Column or <code>-1</code>.
   */
  public int getColumn() {
    return column;
  }

  /**
   * @return Filename.
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return Line or <code>-1</code>.
   */
  public int getLine() {
    return line;
  }

  /**
   * @return An error message.
   */
  public String getMessage() {
    return message;
  }

  public String getEvidence() {
    return evidence == null ? "" : evidence;
  }

  @Override
  public String toString() {
    return (filename + ":" + line + ":" + column + ": " + message + "\n" + getEvidence()).trim();
  }
}
