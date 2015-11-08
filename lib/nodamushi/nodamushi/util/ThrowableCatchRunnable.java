package nodamushi.util;

public class ThrowableCatchRunnable implements Runnable,ThrowableRunnable{

  private final ThrowableRunnable r;
  private Throwable t;

  public ThrowableCatchRunnable(final ThrowableRunnable r){
    this.r = r;
  }


  @Override
  public void run(){
    t = null;
    if(r!=null){
      try{
        r.unsafeRun();
      }catch(final Throwable t){
        this.t = t;
      }
    }
  }


  @Override
  public void unsafeRun() throws Throwable{
    if(r!=null){
      r.unsafeRun();
    }
  }

  public Throwable getError(){
    return t;
  }

  public void clearError(){
    t = null;
  }

  public boolean hasError(){
    return t!=null;
  }
}
