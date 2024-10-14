/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import static io.jooby.MediaType.*;
import static io.jooby.SneakyThrows.throwingConsumer;
import static io.jooby.StatusCode.SERVER_ERROR_CODE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.exception.NotAcceptableException;

/**
 * Global error handler that catches all exceptions, transforms them into <a
 * href="https://www.rfc-editor.org/rfc/rfc7807">RFC7807</a> compliant format and renders the
 * response based on the `Accept` header value. It also sets the appropriate content-type in
 * response (e.g. application/problem+json, application/problem+xml)
 *
 * @author kliushnichenko
 * @since 3.4.2
 */
public class ProblemDetailsHandler extends DefaultErrorHandler {

  private static final String MUTE_CODES_KEY = "muteCodes";
  private static final String MUTE_TYPES_KEY = "muteTypes";
  private static final String LOG_4XX_ERRORS_KEY = "log4xxErrors";

  public static final String ROOT_CONFIG_PATH = "problem.details";
  public static final String ENABLED_KEY = ROOT_CONFIG_PATH + ".enabled";

  private boolean log4xxErrors;

  public ProblemDetailsHandler log4xxErrors() {
    this.log4xxErrors = true;
    return this;
  }

  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    Logger log = ctx.getRouter().getLog();
    if (cause instanceof NotAcceptableException ex) {
      // no matching produce type, respond in html
      var problem = ex.toHttpProblem();

      logProblem(ctx, problem, cause);
      sendHtml(ctx, problem);
      return;
    }

    try {
      var problem = evaluateTheProblem(cause, code);

      logProblem(ctx, problem, cause);

      var type = ctx.accept(List.of(ctx.getRequestType(text), html, text, json, xml));
      ctx.setResponseCode(problem.getStatus());
      problem.getHeaders().forEach(ctx::setResponseHeader);

      if (text.equals(type)) {
        ctx.setResponseType(text).send(problem.toString());
      } else if (html.equals(type)) {
        sendHtml(ctx, problem);
      } else {
        try {
          setResponseType(ctx, type);
          ctx.render(toProblemResponse(problem));
        } catch (NotAcceptableException ex) {
          sendHtml(ctx, problem);
        }
      }
    } catch (Exception ex) {
      log.error("Unexpected error during ProblemDetailsErrorHandler execution", ex);
    }
  }

  private void setResponseType(Context ctx, MediaType type) {
    if (json.equals(type)) {
      ctx.setResponseType(PROBLEM_JSON);
    } else if (xml.equals(type)) {
      ctx.setResponseType(PROBLEM_XML);
    }
  }

  private HttpProblem evaluateTheProblem(Throwable cause, StatusCode statusCode) {
    HttpProblem problem;
    if (cause instanceof HttpProblem httpProblem) {
      problem = httpProblem;
    } else if (cause instanceof HttpProblemMappable problemMappable) {
      problem = problemMappable.toHttpProblem();
    } else {
      var code = statusCode.value();
      if (code == SERVER_ERROR_CODE) {
        problem = HttpProblem.internalServerError();
      } else if (code > SERVER_ERROR_CODE) {
        problem = HttpProblem.valueOf(statusCode, statusCode.reason());
      } else {
        var message = cause.getMessage();
        if (message != null) {
          String details = message.split("\n")[0];
          problem = HttpProblem.valueOf(statusCode, statusCode.reason(), details);
        } else {
          problem = HttpProblem.valueOf(statusCode, statusCode.reason());
        }
      }
    }
    return problem;
  }

  private void sendHtml(Context ctx, HttpProblem problem) {
    String title = problem.getTitle();
    var html =
        new StringBuilder(
                """
        <!doctype html>
        <html>
        <head>
        <meta charset="utf-8">
        <style>
        body {font-family: "open sans",sans-serif; margin-left: 20px;}
        h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}
        h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}
        footer {font-weight: 300; line-height: 44px; margin-top: 10px;}
        hr {background-color: #f7f7f9;}
        div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}
        p {padding-left: 20px;}
        p.tab {padding-left: 40px;}
        </style>
        """)
            .append("<title>")
            .append(problem.getStatus())
            .append("</title>\n")
            .append("<body>\n")
            .append("<h1>")
            .append(title)
            .append("</h1>\n")
            .append("<hr>\n")
            .append("<h2>timestamp: ")
            .append(problem.getTimestamp())
            .append("</h2>\n")
            .append("<h2>type: ")
            .append(problem.getType())
            .append("</h2>\n")
            .append("<h2>title: ")
            .append(title)
            .append("</h2>\n")
            .append("<h2>status: ")
            .append(problem.getStatus())
            .append("</h2>\n");

    if (problem.getInstance() != null) {
      html.append("<h2>instance: ").append(problem.getInstance()).append("</h2>\n");
    }
    if (problem.getDetail() != null) {
      html.append("<h2>detail: ").append(problem.getDetail()).append("</h2>\n");
    }
    if (problem.hasParameters()) {
      html.append("<h2>parameters: ").append(problem.getParameters()).append("</h2>\n");
    }
    if (problem.hasErrors()) {
      html.append("<h2>errors: ").append(problem.getErrors()).append("</h2>\n");
    }

    html.append("</body>\n").append("</html>");

    ctx.setResponseType(MediaType.html).setResponseCode(problem.getStatus()).send(html.toString());
  }

  private Map<String, Object> toProblemResponse(HttpProblem httpProblem) {
    var response = new LinkedHashMap<String, Object>();
    response.put("timestamp", httpProblem.getTimestamp());
    response.put("type", httpProblem.getType());
    response.put("title", httpProblem.getTitle());
    response.put("status", httpProblem.getStatus());
    response.put("detail", httpProblem.getDetail());
    response.put("instance", httpProblem.getInstance());
    if (httpProblem.hasParameters()) {
      response.put("parameters", httpProblem.getParameters());
    }
    if (httpProblem.hasErrors()) {
      response.put("errors", httpProblem.getErrors());
    }
    return response;
  }

  private void logProblem(Context ctx, HttpProblem problem, Throwable cause) {
    var statusCode = StatusCode.valueOf(problem.getStatus());
    var log = ctx.getRouter().getLog();

    if (problem.getStatus() >= SERVER_ERROR_CODE) {
      var msg = buildLogMsg(ctx, problem, statusCode);
      log.error(msg, cause);
    } else {
      if (!isMuted(cause, statusCode) && log4xxErrors) {
        var msg = buildLogMsg(ctx, problem, statusCode);
        if (log.isDebugEnabled()) {
          log.debug(msg, cause);
        } else {
          log.info(msg);
        }
      }
    }
  }

  private String buildLogMsg(Context ctx, HttpProblem problem, StatusCode statusCode) {
    return "%s | %s".formatted(ErrorHandler.errorMessage(ctx, statusCode), problem.toString());
  }

  /**
   * Creates a problem handler from configuration.
   *
   * <pre>{@code
   * problem.details {
   *   enable: true
   *   muteCodes: [401, 106]
   *   muteTypes: ['com.example.MyException']
   * }
   * }</pre>
   *
   * @param conf Configuration.
   * @return Problem handler.
   */
  public static ProblemDetailsHandler from(Config conf) {
    var handler = new ProblemDetailsHandler();
    if (conf.hasPath(ROOT_CONFIG_PATH)) {
      var problemConfig = conf.getConfig(ROOT_CONFIG_PATH);
      if (problemConfig.hasPath(LOG_4XX_ERRORS_KEY)
          && problemConfig.getBoolean(LOG_4XX_ERRORS_KEY)) {
        handler.log4xxErrors();
      }

      if (problemConfig.hasPath(MUTE_CODES_KEY)) {
        problemConfig
            .getIntList(MUTE_CODES_KEY)
            .forEach(code -> handler.mute(StatusCode.valueOf(code)));
      }

      if (problemConfig.hasPath(MUTE_TYPES_KEY)) {
        var classLoader = ProblemDetailsHandler.class.getClassLoader();
        problemConfig
            .getStringList(MUTE_TYPES_KEY)
            .forEach(
                throwingConsumer(
                    className ->
                        handler.mute(
                            (Class<? extends Exception>) classLoader.loadClass(className))));
      }
    }

    return handler;
  }
}
