package java9;

@Interceptable
public interface UserService {
  @EnsureRole("manager")
  public void addUser(String userName, String userMailAddress, boolean admin);
}
