package java9;
import java.lang.reflect.AnnotatedElement;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.stream.IntStream;

public class Main {
  @SuppressWarnings("unused")
  private static void checkRole(AnnotatedElement annotatedElement, EnsureRole ensureRole) {
    // check role here
    //System.out.println("verify that role is " + ensureRole.value() + " when calling " + annotatedElement);
  }
  
  private static final MethodHandle CHECK_ROLE;
  static {
    try {
      CHECK_ROLE = MethodHandles.lookup().findStatic(Main.class,
          "checkRole",
          MethodType.methodType(void.class, AnnotatedElement.class, EnsureRole.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
  
  public static void main(String[] args) {
    //System.out.println(Magic.asMHV2(Main::checkRole));
    
    Container.addAdvice((AnnotatedElement annotatedElement, MethodHandle mh) -> {
      EnsureRole ensureRole = annotatedElement.getAnnotation(EnsureRole.class);
      if (ensureRole == null) {
        return mh;
      }
      MethodHandle combiner = MethodHandles.insertArguments(CHECK_ROLE, 0, annotatedElement, ensureRole);
      return MethodHandles.foldArguments(mh, combiner);
    });
    
    UserService userService = new UserServiceImpl();
    for(int i=0; i < 10_000_000; i++) {
      userService.addUser("Darth Vador", "1 Force Street, Death Star", true);
    }
  }
}
