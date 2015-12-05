package org.jooby.pac4j;

import static org.junit.Assert.assertEquals;

import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.pac4j.core.authorization.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.credentials.UsernamePasswordCredentials;
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator;
import org.pac4j.http.profile.HttpProfile;

public class RequireAdminAuthWithClassFeature extends ServerFeature {

  public static class AdminRole implements UsernamePasswordAuthenticator {

    @Override
    public void validate(final UsernamePasswordCredentials credentials) {
      final HttpProfile profile = new HttpProfile();
      String username = credentials.getUsername();
      profile.setId(username);
      profile.addAttribute(CommonProfile.USERNAME, username);
      credentials.setUserProfile(profile);
      profile.addPermission("admin");
    }

  }

  public static class RequireAdmin<U extends UserProfile> implements Authorizer<U> {

    @Override
    public boolean isAuthorized(final WebContext context, final U profile) {
      return profile.getPermissions().contains("admin");
    }

  }

  {

    use(new Auth()
        .form("*", AdminRole.class)
        .authorizer("admin", "/admin/**", RequireAdmin.class));

    get("/", req -> req.path());

    get("/admin", req -> req.path());
  }

  @Test
  public void auth() throws Exception {
    request()
        .get("/auth?username=test&password=test")
        .expect("/");

    request()
        .get("/admin")
        .expect("/admin");
  }

  @Test
  public void redirectToLoginPage() throws Exception {
    request()
        .dontFollowRedirect()
        .get("/auth/form")
        .expect(302)
        .header("Location", "/login");
  }

  @Test
  public void loginPage() throws Exception {
    request()
        .get("/auth/form")
        .expect(rsp -> {
          Document html = Jsoup.parse(rsp);
          assertEquals("Login Page", html.getElementsByTag("title").iterator().next().text());
        });
  }

}
