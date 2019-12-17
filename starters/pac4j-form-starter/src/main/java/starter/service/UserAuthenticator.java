package starter.service;

import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import starter.domain.User;
import starter.domain.UserRepository;

public class UserAuthenticator implements Authenticator<UsernamePasswordCredentials> {
  private final UserRepository repository;
  private final PasswordService passwordService;

  public UserAuthenticator(UserRepository repository, PasswordService passwordService) {
    this.repository = repository;
    this.passwordService = passwordService;
  }

  @Override public void validate(UsernamePasswordCredentials credentials,
      WebContext context) {
    String username = credentials.getUsername();
    if (username == null || username.trim().length() == 0) {
      throw new CredentialsException("Username and password cannot be blank");
    }

    User userInfo = repository.findByUsername(username);
    if (userInfo == null) {
      throw new CredentialsException("Username or password invalid");
    }

    if (!passwordService.checkPw(credentials.getPassword(), userInfo.getPassword())) {
      throw new CredentialsException("Username or password invalid");
    }

    CommonProfile profile = new CommonProfile();
    profile.setId(username);
    profile.addAttribute(Pac4jConstants.USERNAME, username);
    credentials.setUserProfile(profile);
  }

}
