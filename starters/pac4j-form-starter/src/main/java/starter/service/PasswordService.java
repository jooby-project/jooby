package starter.service;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordService {
  public boolean checkPw(String password, String hashPassword) {
    return BCrypt.checkpw(password, hashPassword);
  }

  public String encryptPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(10));
  }
}
