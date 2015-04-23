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
package org.skife.jdbi.v2;

import static org.skife.jdbi.rewriter.colon.ColonStatementLexer.DOUBLE_QUOTED_TEXT;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexer.ESCAPED_TEXT;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexer.LITERAL;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexer.NAMED_PARAM;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexer.POSITIONAL_PARAM;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexer.QUOTED_TEXT;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.skife.jdbi.org.antlr.runtime.ANTLRStringStream;
import org.skife.jdbi.org.antlr.runtime.Token;
import org.skife.jdbi.rewriter.colon.ColonStatementLexer;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.RewrittenStatement;
import org.skife.jdbi.v2.tweak.StatementRewriter;

import com.google.common.base.Strings;

/**
 * Statement rewriter which replaces named parameter tokens of the form :tokenName
 * <p/>
 * This is the default statement rewriter.
 *
 * This is a copy of the {@link ColonPrefixNamedParamStatementRewriter} with sql expansion for
 * multi-value argument.
 *
 * TODO: find sometime and send a pull request to include these changes. Here we are accessing to
 * some packages protected classes, which isn't good.
 */
public class ExpandedStmtRewriter implements StatementRewriter
{
  /**
   * Munge up the SQL as desired. Responsible for figuring out ow to bind any
   * arguments in to the resultant prepared statement.
   *
   * @param sql The SQL to rewrite
   * @param params contains the arguments which have been bound to this statement.
   * @param ctx The statement context for the statement being executed
   * @return somethign which can provde the actual SQL to prepare a statement from
   *         and which can bind the correct arguments to that prepared statement
   */
  @Override
  public RewrittenStatement rewrite(final String sql, final Binding params,
      final StatementContext ctx)
  {
    final ParsedStatement stmt = new ParsedStatement();
    try {
      final String parsedSql = parseString(sql, stmt, params);
      return new MyRewrittenStatement(parsedSql, stmt, ctx);
    } catch (IllegalArgumentException e) {
      throw new UnableToCreateStatementException(
          "Exception parsing for named parameter replacement", e, ctx);
    }

  }

  String parseString(final String sql, final ParsedStatement stmt, final Binding params)
      throws IllegalArgumentException {
    StringBuilder b = new StringBuilder();
    ColonStatementLexer lexer = new ColonStatementLexer(new ANTLRStringStream(sql));
    Token t = lexer.nextToken();
    int pos = 0;
    while (t.getType() != ColonStatementLexer.EOF) {
      switch (t.getType()) {
        case LITERAL:
          b.append(t.getText());
          break;
        case NAMED_PARAM:
          String pname = t.getText().substring(1, t.getText().length());
          stmt.addNamedParamAt(pname);
          Argument arg = params.forName(pname);
          if (arg instanceof IterableArgument) {
            // expand iterable
            int size = ((IterableArgument) arg).size();
            b.append(Strings.repeat("?, ", size));
            b.setLength(b.length() - 2);
          } else {
            b.append("?");
          }
          break;
        case QUOTED_TEXT:
          b.append(t.getText());
          break;
        case DOUBLE_QUOTED_TEXT:
          b.append(t.getText());
          break;
        case POSITIONAL_PARAM:
          Argument posarg = params.forPosition(pos);
          if (posarg instanceof IterableArgument) {
            // expand iterable
            int size = ((IterableArgument) posarg).size();
            b.append(Strings.repeat("?, ", size));
            b.setLength(b.length() - 2);
            pos += size;
          } else {
            b.append("?");
            pos += 1;
          }
          stmt.addPositionalParamAt();
          break;
        case ESCAPED_TEXT:
          b.append(t.getText().substring(1));
          break;
        default:
          break;
      }
      t = lexer.nextToken();
    }
    return b.toString();
  }

  private static class MyRewrittenStatement implements RewrittenStatement
  {
    private final String sql;
    private final ParsedStatement stmt;
    private final StatementContext context;

    public MyRewrittenStatement(final String sql, final ParsedStatement stmt,
        final StatementContext ctx)
    {
      this.context = ctx;
      this.sql = sql;
      this.stmt = stmt;
    }

    @Override
    public void bind(final Binding params, final PreparedStatement statement) throws SQLException
    {
      if (stmt.positionalOnly) {
        // no named params, is easy
        boolean finished = false;
        int i = 0;
        while (!finished) {
          final Argument a = params.forPosition(i);
          if (a != null) {
            try {
              a.apply(i + 1, statement, this.context);
              Integer pos = (Integer) this.context.getAttribute("position");
              i += Optional.ofNullable(pos).orElse(1);
              this.context.setAttribute("position", null);
            } catch (SQLException e) {
              throw new UnableToExecuteStatementException(
                  String.format(
                      "Exception while binding positional param at (0 based) position %d",
                      i), e, context);
            }
          }
          else {
            finished = true;
          }
        }
      }
      else {
        // List<String> named_params = stmt.params;
        int i = 0;
        for (String named_param : stmt.params) {
          if ("*".equals(named_param)) {
            continue;
          }
          Argument a = params.forName(named_param);
          if (a == null) {
            a = params.forPosition(i);
          }

          if (a == null) {
            String msg = String.format("Unable to execute, no named parameter matches " +
                "\"%s\" and no positional param for place %d (which is %d in " +
                "the JDBC 'start at 1' scheme) has been set.",
                named_param, i, i + 1);
            throw new UnableToExecuteStatementException(msg, context);
          }

          try {
            a.apply(i + 1, statement, this.context);
          } catch (SQLException e) {
            throw new UnableToCreateStatementException(String.format(
                "Exception while binding '%s'",
                named_param), e, context);
          }
          i++;
        }
      }
    }

    @Override
    public String getSql()
    {
      return sql;
    }
  }

  static class ParsedStatement
  {
    private boolean positionalOnly = true;
    private List<String> params = new ArrayList<String>();

    public void addNamedParamAt(final String name)
    {
      positionalOnly = false;
      params.add(name);
    }

    public void addPositionalParamAt()
    {
      params.add("*");
    }
  }
}
