package starter;

import com.typesafe.config.Config;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ModelAndView;
import io.jooby.WebVariables;
import io.jooby.handlebars.HandlebarsModule;
import io.jooby.json.JacksonModule;
import io.jooby.pac4j.Pac4jModule;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

public class App extends Jooby {

  {
    decorator(new WebVariables());

    /** JSON: */
    install(new JacksonModule());

    /** Template engine: */
    install(new HandlebarsModule());

    assets("/css/*");
    assets("/images/*");

    get("/login", ctx -> new ModelAndView("login.hbs"));

    install(new Pac4jModule()
        /** Login with facebook: */
        .client("/facebook", conf ->
            new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"))
        )
        /** Login with twitter: */
        .client("/twitter", conf ->
            new TwitterClient(conf.getString("twitter.key"), conf.getString("twitter.secret"))
        )
        /** Login with google: */
        .client("/google", conf -> {
          OidcConfiguration oidc = new OidcConfiguration();
          oidc.setClientId(conf.getString("oidc.clientId"));
          oidc.setSecret(conf.getString("oidc.secret"));
          //          oidc.setDiscoveryURI(conf.getString("oidc.discoveryURI"));
          oidc.addCustomParam("prompt", "consent");
          oidc.setUseNonce(true);
          return new GoogleOidcClient(oidc);
        })
        /** Login with JWT: */
        .client("/api/*", conf -> {
          ParameterClient client = new ParameterClient("token",
              new JwtAuthenticator(new SecretSignatureConfiguration(conf.getString("jwt.salt"))));
          client.setSupportGetRequest(true);
          client.setSupportPostRequest(true);
          return client;
        })
        /** Fallback to form login: */
        .client(conf ->
            new FormClient("/login", ((credentials, context) -> {
              // Create default profile:
              String username = ((UsernamePasswordCredentials) credentials).getUsername();
              final CommonProfile profile = new CommonProfile();
              profile.setId(username);
              profile.addAttribute(Pac4jConstants.USERNAME, username);
              profile.addAttribute(CommonProfileDefinition.DISPLAY_NAME, username);
              credentials.setUserProfile(profile);
            }))
        ));

    /** Protected pages: */
    get("/", ctx -> {
      CommonProfile profile = ctx.getUser();
      return new ModelAndView("profile.hbs")
          .put("jwtToken", generateToken(getConfig(), profile))
          .put("profile", profile);
    });

    /** Generate Token for user: */
    get("/generate-token", ctx -> {
      String token = generateToken(getConfig(), ctx.getUser());
      return ctx.setResponseType(MediaType.text).send(token);
    });

    /** API protected via JWT: */
    get("/api/profile", ctx -> {
      CommonProfile profile = ctx.getUser();
      return profile.getAttributes();
    });
  }

  private String generateToken(Config conf, CommonProfile profile) {
    JwtGenerator<CommonProfile> jwtGenerator = new JwtGenerator<>(
        new SecretSignatureConfiguration(conf.getString("jwt.salt")));
    return jwtGenerator.generate(profile);
  }

  public static void main(String[] args) {
    runApp(args, App::new);
  }
}
