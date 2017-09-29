package org.jooby.pac4j2;

import org.jooby.test.ServerFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class RequireAdminAuthWithClassFeature extends ServerFeature {

  public static class AdminRole implements Authenticator<UsernamePasswordCredentials> {

    @Override
    public void validate(final UsernamePasswordCredentials credentials, final WebContext context) {
      final CommonProfile profile = new CommonProfile();
      String username = credentials.getUsername();
      profile.setId(username);
      profile.addAttribute(Pac4jConstants.USERNAME, username);
      credentials.setUserProfile(profile);
      profile.addPermission("admin");
    }

  }

  public static class RequireAdmin<U extends CommonProfile> implements Authorizer<U> {

    @Override
    public boolean isAuthorized(final WebContext context, final List<U> profiles) {
      return profiles.get(0).getPermissions().contains("admin");
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
