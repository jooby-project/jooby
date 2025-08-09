/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import io.jooby.Context;
import io.jooby.Jooby;

/**
 * Security Scheme.
 *
 * @securityScheme.name myOauth2Security
 * @securityScheme.type oauth2
 * @securityScheme.in header
 * @securityScheme.paramName X-Auth
 * @securityScheme.flows.implicit.authorizationUrl http://url.com/auth
 * @securityScheme.flows.implicit.scopes.name [write:pets, read:pets]
 * @securityScheme.flows.implicit.scopes.description [modify pets in your account, read your pets]
 * @securityScheme.name myOauth
 * @securityScheme.type oauth2
 * @securityScheme.in header
 * @securityScheme.flows.implicit.authorizationUrl http://url.com/auth
 * @securityScheme.flows.implicit.scopes user:read
 */
public class SecuritySchemaDocApp extends Jooby {

  {
    /*
     * Pets.
     *
     * @securityRequirement myOauth2Security read:pets
     */
    get("/pets", Context::getRequestPath);

    /*
     * Create Posts.
     *
     * @security myOauth2Security [read:pets, write:pets]
     */
    post("/pets", Context::getRequestPath);
  }
}
