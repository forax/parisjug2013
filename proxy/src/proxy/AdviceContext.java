package proxy;

import java.lang.reflect.AnnotatedElement;

public interface AdviceContext {
  public Object call(AnnotatedElement annotatedElement, Object receiver, Object[] args);
}
