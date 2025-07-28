/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import edu.umd.cs.findbugs.annotations.NonNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

/** Search options. */
public class QueryBeanDoc {
  public static final int DEFAULT_OFFSET = 0;

  /** This comment will be ignored. */
  private String fq;

  /** Offset, used for paging. */
  @Min(0)
  // Something
  private int offset = DEFAULT_OFFSET;

  private int limit;

  // Odd position of annotations
  @NotEmpty
  /**
   * Filter query. Works like internal filter.
   *
   * @return Filter query. Works like internal filter.
   */
  @NonNull public String getFq() {
    return fq;
  }

  public void setFq(String fq) {
    this.fq = fq;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
}
