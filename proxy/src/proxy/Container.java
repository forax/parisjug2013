package proxy;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

public class Container {
  AdviceContextImpl adviceContext = new AdviceContextImpl((AnnotatedElement annotatedElement, Object receiver, Object[] args, AdviceContext next) -> {
    try {
      return ((Method)annotatedElement).invoke(receiver, args);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch(InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException)cause;
      }
      if (cause instanceof Error) {
        throw (Error)cause;
      }
      throw new UndeclaredThrowableException(cause);
    }
  }, null);
  
  static class AdviceContextImpl implements AdviceContext {
    private final Advice advice;
    private final AdviceContext next;
    
    AdviceContextImpl(Advice advice, AdviceContext next) {
      this.advice = advice;
      this.next = next;
    }

    @Override
    public Object call(AnnotatedElement annotatedElement, Object receiver, Object[] args) {
      return advice.chain(annotatedElement, receiver, args, next);
    }    
  }
  
  public <S> S getService(Class<S> serviceInterface, Class<? extends S> serviceImplementation) {
    S impl;
    try {
      impl = serviceImplementation.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }
    
    class ServiceInvocationHandler implements InvocationHandler {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return adviceContext.call(method, impl, args);
      }
    }
    return serviceInterface.cast(
        Proxy.newProxyInstance(serviceInterface.getClassLoader(),
            new Class<?>[] { serviceInterface},
            new ServiceInvocationHandler()));
  }
  
  public void addAdvice(Advice advice) {
    adviceContext = new AdviceContextImpl(advice, adviceContext);
  }
}
