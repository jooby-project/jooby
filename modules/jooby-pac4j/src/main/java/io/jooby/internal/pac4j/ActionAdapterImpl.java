/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;

import io.jooby.StatusCode;
import io.jooby.pac4j.Pac4jContext;

public class ActionAdapterImpl implements HttpActionAdapter {

  @Override
  public Object adapt(HttpAction action, WebContext context) {
    if (action == null) {
      throw new TechnicalException("No action provided");
    }
    var statusCode = StatusCode.valueOf(action.getCode());
    var ctx = ((Pac4jContext) context).getContext();
    if (action instanceof WithLocationAction) {
      return ctx.sendRedirect(statusCode, ((WithLocationAction) action).getLocation());
    } else {
      ctx.setResponseCode(statusCode);
      throw action;
    }
  }
}
