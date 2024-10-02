/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.*;
import io.jooby.exception.*;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.jooby.MediaType.*;
import static io.jooby.StatusCode.*;

public class ProblemDetailsErrorHandler extends DefaultErrorHandler {

  private boolean log4xxErrors;

  public ProblemDetailsErrorHandler log4xxErrors() {
    this.log4xxErrors = true;
    return this;
  }

  @Override
  public void apply(@NonNull Context ctx, @NonNull Throwable cause, @NonNull StatusCode code) {
    Logger log = ctx.getRouter().getLog();
    if (cause instanceof NotAcceptableException ex) {
      // no matching produce type, respond in html
      var problem = HttpProblem.valueOf(ex.getStatusCode(),
          NOT_ACCEPTABLE.reason(),
          "Server cannot produce a response matching the list of " +
          "acceptable values defined in the request's 'Accept' header"
      );

      logProblem(ctx, problem, cause);
      sendHtml(ctx, problem);
      return;
    }

    try {
      HttpProblem problem = evaluateTheProblem(ctx, cause, code);

      logProblem(ctx, problem, cause);

      MediaType type = ctx.accept(Arrays.asList(ctx.getRequestType(), html, text, json, xml));
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

  private HttpProblem evaluateTheProblem(Context ctx, Throwable cause, StatusCode statusCode) {
    HttpProblem problem;
    if (cause instanceof HttpProblem httpProblem) {
      problem = httpProblem;
    } else if (cause instanceof StatusCodeException ex) {
      problem = handleStatusCodeExceptionSuccessors(ex, ctx);
    } else {
      var code = statusCode.value();
      if (code == 500) {
        problem = HttpProblem.internalServerError();
      } else if (code > 500) {
        problem = HttpProblem.valueOf(statusCode, statusCode.reason());
      } else {
        if (cause.getMessage() != null) {
          String details = cause.getMessage().split("\n")[0];
          problem = HttpProblem.valueOf(statusCode, statusCode.reason(), details);
        } else {
          problem = HttpProblem.valueOf(statusCode, statusCode.reason());
        }
      }
    }
    return problem;
  }

  private HttpProblem handleStatusCodeExceptionSuccessors(StatusCodeException ex, Context ctx) {
    HttpProblem problem;
    StatusCode code = ex.getStatusCode();

    if (ex instanceof InvalidCsrfToken) {
      problem = HttpProblem.valueOf(code,
          "Invalid CSRF token",
          "CSRF token '" + ex.getMessage() + "' is invalid");
    } else if (ex instanceof MethodNotAllowedException) {
      problem = HttpProblem.valueOf(code,
          METHOD_NOT_ALLOWED.reason(),
          "HTTP method '" + ex.getMessage() + "' for the requested path '" + ctx.getRequestPath() +
          "' is not allowed");
    } else if (ex instanceof NotFoundException) {
      problem = HttpProblem.valueOf(
          code, NOT_FOUND.reason(), "Route not found. Please verify request 'path'");
    } else if (ex instanceof TypeMismatchException) {
      problem = HttpProblem.valueOf(code, "Type Mismatch", ex.getMessage());
    } else if (ex instanceof UnsupportedMediaType) {
      problem = HttpProblem.valueOf(code,
          UNSUPPORTED_MEDIA_TYPE.reason(),
          "Media type '" + ex.getMessage() + "' is not supported");
    } else {
      problem = HttpProblem.valueOf(code, ex.getMessage());
    }

    return problem;
  }

  private void sendHtml(Context ctx, HttpProblem problem) {
    String title = problem.getTitle();
    StringBuilder html = new StringBuilder("""
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
        .append("<title>").append(problem.getStatus()).append("</title>\n")
        .append("<body>\n")
        .append("<h1>").append(title).append("</h1>\n")
        .append("<hr>\n")
        .append("<h2>timestamp: ").append(problem.getTimestamp()).append("</h2>\n")
        .append("<h2>type: ").append(problem.getType()).append("</h2>\n")
        .append("<h2>title: ").append(title).append("</h2>\n")
        .append("<h2>status: ").append(problem.getStatus()).append("</h2>\n");

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

    ctx.setResponseType(MediaType.html)
        .setResponseCode(problem.getStatus())
        .send(html.toString());
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
    StatusCode statusCode = StatusCode.valueOf(problem.getStatus());
    var log = ctx.getRouter().getLog();

    if (problem.getStatus() >= 500) {
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
}
