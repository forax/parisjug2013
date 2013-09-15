package proxy;

public class UserServiceImpl implements UserService {
  private static int COUNTER;
  
  @Override
  public void addUser(String userName, String userMailAddress, boolean admin) {
    // add a new user to the database
    // send a mail to invite the new user
    COUNTER++;
  }
}
