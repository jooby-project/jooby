/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;

public class ActionAdapterImpl implements HttpActionAdapter<Object, Pac4jContext> {

  @Override public Object adapt(HttpAction action, Pac4jContext context) {
    if (action == null) {
      // It is unclear when we reach this state:
      throw new TechnicalException("No action provided");
    }
    StatusCode statusCode = StatusCode.valueOf(action.getCode());
    Context rsp = context.getContext();
    if (action instanceof WithLocationAction) {
      return rsp.sendRedirect(statusCode, ((WithLocationAction) action).getLocation());
    } else if (action instanceof WithContentAction) {
      return rsp.setResponseCode(statusCode).send(((WithContentAction) action).getContent());
    } else {
      throw action;
    }
  }
}
