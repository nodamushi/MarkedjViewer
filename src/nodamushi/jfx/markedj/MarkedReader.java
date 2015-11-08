package nodamushi.jfx.markedj;

import static java.util.Objects.*;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.PKIXRevocationChecker.Option;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.github.gitbucket.markedj.Marked;
import io.github.gitbucket.markedj.Options;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import nodamushi.nio.FileUtil;
import nodamushi.nio.FileWatcher;
import nodamushi.nio.FileWatcher.PathWatchEvent;

public class MarkedReader{

  private static class WeakConsumer implements Consumer<PathWatchEvent>{

    private final WeakReference<MarkedReader> reader;
    private Path path;

    public WeakConsumer(final MarkedReader r){
      reader = new WeakReference<>(r);
      path = r.file;
    }


    @Override
    public void accept(final PathWatchEvent t){
      final MarkedReader r = reader.get();
      if(r==null){
        t.getWatcher().unwatchFile(path, this);
      }else{
        r.parse();
      }

    }

  }

  public static final Options DEFAULT_OPTIONS=new Options();


  static{
    DEFAULT_OPTIONS.setLangPrefix("");
  }
  private volatile boolean initialized = false;
  private final Path file;
  private final AtomicReference<WeakConsumer> ref;
  private volatile Options options = null;

  public MarkedReader(final Path file){
    this.file =requireNonNull(file,"file");
    ref = new AtomicReference<>(null);
  }

  public Path getDirectory(){
    return file.getParent();
  }

  public Path getFile(){
    return file;
  }

  public void watchFile(){
    final WeakConsumer c = new WeakConsumer(this);
    if(ref.compareAndSet(null, c)){
      FileWatcher.getDefault().watchFile(file, c);
      if(!initialized){
        parse();
      }
    }
  }

  public void setOptions(final Options option){
    options = option;
    if(initialized && ref.get()!=null){
      parse();
    }
  }

  public void unwatchFile(){
    if(ref.get()!=null){
      FileWatcher.getDefault().unwatchFile(file, ref.get());
      ref.set(null);
    }
  }


  public void parse(){
    final String code;
    try {
      if(Files.isReadable(file)){
        final Options o = options==null?DEFAULT_OPTIONS:options;
        code = Marked.marked(FileUtil.readText(file),o);
      }else{
        code = null;
      }
    } catch (final IOException e) {
      e.printStackTrace();
      return;
    }
    if(code!=null){
      initialized=true;
      if(Platform.isFxApplicationThread()){
        setCode(code);
      }else{
        Platform.runLater(()->setCode(code));
      }
    }
  }



  /**
   *
   * @return
   */
  public final ReadOnlyStringProperty codeProperty(){
    return codeWrapper().getReadOnlyProperty();
  }

  public final String getCode(){
    return codeWrapper.get();
  }

  protected final void setCode(final String value){
    codeWrapper().set(value);
  }

  protected final ReadOnlyStringWrapper codeWrapper(){
    return codeWrapper;
  }

  private final ReadOnlyStringWrapper codeWrapper = new ReadOnlyStringWrapper(this, "code", "");

}
