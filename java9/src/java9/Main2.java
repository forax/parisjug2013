package java9;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Main2 {
  @SuppressWarnings("unused")
  private static void checkRole(AnnotatedElement annotatedElement, EnsureRole ensureRole) {
    // check role here
    //System.out.println("verify that role is " + ensureRole.value() + " when calling " + annotatedElement);
  }
  
  private static final MethodHandle CHECK_ROLE;
  static {
    try {
      CHECK_ROLE = MethodHandles.lookup().findStatic(Main2.class,
          "checkRole",
          MethodType.methodType(void.class, AnnotatedElement.class, EnsureRole.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }
  
  public static void main(String[] args) {
    Container.addAdvice((AnnotatedElement annotatedElement, MethodHandle mh) -> {
      EnsureRole ensureRole = annotatedElement.getAnnotation(EnsureRole.class);
      if (ensureRole == null) {
        return mh;
      }
      MethodHandle combiner = MethodHandles.insertArguments(CHECK_ROLE, 0, annotatedElement, ensureRole);
      return MethodHandles.foldArguments(mh, combiner);
    });
    
    HashMap<Class<?>, Object> injectionMap = new HashMap<>();
    injectionMap.put(Mailer.class, new MailerImpl());
    Container.addAdvice((AnnotatedElement annotatedElement, MethodHandle mh) -> {
      if (!annotatedElement.isAnnotationPresent(Inject.class)) {
        return mh;
      }
      Class<?> type = mh.type().returnType();
      return MethodHandles.dropArguments(
          MethodHandles.constant(type, injectionMap.get(type)),
          0, mh.type().parameterType(0));
    });
    
    
    UserService userService = new UserServiceImpl();
    //for(int i=0; i < 10_000_000; i++) {
      userService.addUser("Darth Vador", "1 Force Street, Death Star", true);
    //}
  }
}
