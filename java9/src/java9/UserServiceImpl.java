package java9;

//@Intercepted
public class UserServiceImpl implements UserService {
  private static int COUNTER;
  
  //@Inject
  //private Mailer mailer;
  
  @Override
  public void addUser(String userName, String userMailAddress, boolean admin) {
    // add a new user to the database
    // send a mail to invite the new user
    //mailer.sendAMail(userMailAddress, "hello " + userName+", ...");
    COUNTER++;
    
    if (COUNTER == 1_000_000) {   // nice stack trace ??
      new Throwable().printStackTrace();
    }
  }
}
