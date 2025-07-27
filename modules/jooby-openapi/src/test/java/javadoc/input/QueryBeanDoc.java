/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

/** Search options. */
public class QueryBeanDoc {
  private String fq;
  private int offset;
  private int limit;

  public String getFq() {
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
