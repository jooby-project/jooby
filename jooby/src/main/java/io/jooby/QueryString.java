package io.jooby;

public class QueryString extends Value.Object {
  public static final QueryString EMPTY = new QueryString("");

  private final String queryString;

  public QueryString(String queryString) {
    this.queryString = queryString;
  }

  /**
   * Query string with the leading <code>?</code> or empty string.
   *
   * @return Query string with the leading <code>?</code> or empty string.
   */
  public String queryString() {
    return queryString;
  }
}
