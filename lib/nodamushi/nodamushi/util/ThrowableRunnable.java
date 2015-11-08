package nodamushi.util;

@FunctionalInterface
public interface ThrowableRunnable{
  public void unsafeRun()throws Throwable;
}
