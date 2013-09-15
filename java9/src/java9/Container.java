package java9;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;

public class Container {
  private static final ArrayList<Advice> advices = new ArrayList<>();
  
  public static CallSite bootstrap(Lookup lookup, String name, MethodType methodType, MethodHandle impl) {
    AnnotatedElement annotatedElement = Magic.reflect(impl);
    MethodHandle mh = impl;
    for(Advice advice: advices) {
      mh = advice.chain(annotatedElement, mh);
    }
    return new ConstantCallSite(mh);
  }
  
  public static void addAdvice(Advice interceptor) {
    advices.add(interceptor);
  }
}
