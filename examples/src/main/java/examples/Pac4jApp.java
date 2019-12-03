/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.Session;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.pac4j.Pac4jContext;
import io.jooby.json.JacksonModule;
import io.jooby.pac4j.Pac4jModule;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

import java.util.Optional;

public class Pac4jApp extends Jooby {

  {
    final FacebookClient facebookClient = new FacebookClient("145278422258960",
        "be21409ba8f39b5dae2a7de525484da8");
    TwitterClient twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA",
        "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");
    final OidcConfiguration oidcConfiguration = new OidcConfiguration();
    oidcConfiguration
        .setClientId("343992089165-sp0l1km383i8cbm2j5nn20kbk5dk8hor.apps.googleusercontent.com");
    oidcConfiguration.setSecret("uR3D8ej1kIRPbqAFaxIE3HWh");
    oidcConfiguration
        .setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
    oidcConfiguration.setUseNonce(true);
    //oidcClient.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);
    oidcConfiguration.addCustomParam("prompt", "consent");
    final OidcClient oidcClient = new OidcClient(oidcConfiguration);

    decorator(next -> ctx -> {
      try {
        getLog().info("{} {} sid: {} {}", ctx.getMethod(), ctx.getRequestURL(),
            ctx.cookie("jooby.sid").toOptional().orElse(""), requestedUrl(ctx));
        return next.apply(ctx);
      } finally {
        getLog().info("    {} {} {} sid: {} {}", ctx.getMethod(), ctx.getRequestURL(),
            ctx.getResponseCode(),
            Optional.ofNullable(ctx.sessionOrNull()).map(Session::getId).orElse(""), requestedUrl(ctx));
      }
    });

//    setContextPath("/myapp");

    install(new HandlebarsModule());

    get("/login", ctx -> new ModelAndView("login.hbs"));

    install(new Pac4jModule()
//        .client("/google", conf -> oidcClient)
//        .client("/twitter", conf -> twitterClient)
//        .client(conf -> new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator()))
    );

    install(new JacksonModule());

    get("/", ctx -> new ModelAndView("pac4j.hbs").put("user", ctx.getUser()));

    get("/api", ctx -> ctx.getUser());

    get("/api/v1", ctx -> ctx.getUser());
  }

  private Object requestedUrl(Context ctx) {
    Pac4jContext pac4jContext = Pac4jContext.create(ctx);
    return pac4jContext.getSessionStore().get(pac4jContext, Pac4jConstants.REQUESTED_URL).map(it-> {
      if (it instanceof WithLocationAction) {
        return ((WithLocationAction) it).getLocation();
      } else {
        return it.toString();
      }
    }).orElse("null");
  }

  public static void main(String[] args) {
    runApp(args, Pac4jApp::new);
  }
}
