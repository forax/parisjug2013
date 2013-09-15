package proxy;

import java.lang.reflect.AnnotatedElement;

public class Main {
  private static void checkRole(AnnotatedElement annotatedElement, EnsureRole ensureRole) {
    // check role here
    //System.out.println("verify that role is " + ensureRole.value() + " when calling " + annotatedElement);
  }
  
  public static void main(String[] arguments) {
    Container container = new Container();
    container.addAdvice((AnnotatedElement annotatedElement, Object receiver, Object[] args, AdviceContext context) -> {
        EnsureRole ensureRole = annotatedElement.getAnnotation(EnsureRole.class);
        if (ensureRole != null) {
          checkRole(annotatedElement, ensureRole);
        }
        return context.call(annotatedElement, receiver, args);
      });
    
    UserService userService = container.getService(UserService.class, UserServiceImpl.class);
    for(int i=0; i < 10_000_000; i++) {
      userService.addUser("Darth Vador", "1 Force Street, Death Star", true);
    }
  }
}
