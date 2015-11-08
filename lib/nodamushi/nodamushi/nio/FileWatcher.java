package nodamushi.nio;

import static java.nio.file.Files.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Collections.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import nodamushi.util.ThrowableCatchRunnable;


/**
 * 特定のファイルを監視したいために作ったクラス。<br/>
 * 特定のディレクトリの内容も監視可能<br/>
 * すべてのFileWatcherは一つのFileWathcher用のスレッドで動作する。<br/>
 * (ただし、監視待機しているときを除く)
 * @author nodamushi
 *
 */
public class FileWatcher{

  protected static enum WatchType{
    FILE,DIRECTORY
  }

  /**
   * 監視しているディレクトリもしくはファイルに変化があったときに発効されます。<br/>
   * PathWatchEventはFileWatchEventか、DirectoryWatchEventのどちらのクラスになります
   * @author nodamushi
   *
   */
  public static class PathWatchEvent{
    private final FileWatcher watcher;
    private final Path key;
    private final WatchType type;
    private PathWatchEvent(final FileWatcher w,final Path key,final WatchType type){
      watcher =requireNonNull(w);
      this.key = requireNonNull(key,"key");
      this.type = requireNonNull(type,"type");
    }
    public final Path getKey(){return key;}
    protected final WatchType getType(){return type; }
    public final boolean isDirectoryEvent(){return type == WatchType.DIRECTORY;}
    public final boolean isFileEvent(){return type == WatchType.FILE;}
    public final FileWatcher getWatcher(){return watcher;}
  }
  /**
   * 監視しているファイルに変化があった際に発効されます
   * @author nodamushi
   *
   */
  public static class FileWatchEvent extends PathWatchEvent{
    private final Kind<?> kind;
    private FileWatchEvent(final FileWatcher w,final Path key,final Kind<?> kind){
      super(w,key,WatchType.FILE);
      this.kind = requireNonNull(kind,"kind");
    }
    public final Kind<?> getKind(){ return kind; }
  }

  /**
   * 監視しているディレクトリに変化があった際に発効されます
   * @author nodamushi
   *
   */
  public static class DirectoryWatchEvent extends PathWatchEvent{
    private final List<FileWatchEvent> fileEvents;
    private DirectoryWatchEvent(final FileWatcher w,final Path key,final List<FileWatchEvent> fileEvents){
      super(w,key,WatchType.DIRECTORY);
      this.fileEvents =Collections.unmodifiableList(fileEvents);
    }
    public final List<FileWatchEvent> getFileEvents(){
      return fileEvents;
    }
  }



  private static final Kind<?>[] KINDS={ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY};

  private final WatchService watcher;
  private final List<Key> fileList,dirList;
  private final Runnable run;
  private volatile boolean running=true;
  private volatile Thread thread;

  public FileWatcher() throws IOException{
    watcher = FileSystems.getDefault().newWatchService();
    fileList = new ArrayList<>();
    dirList = new ArrayList<>();
    run= ()->{
      try {
        final AtomicBoolean cancel = new AtomicBoolean(false);
        while(running){
          final WatchKey take = watcher.take();
          final Path path = (Path)take.watchable();
          cancel.set(false);
          final Future<?> f = submit(() ->{
            final List<WatchEvent<?>> pollEvents = take.pollEvents();

            final int dirIndex = binarySearch(dirList, path);
            final List<FileWatchEvent> fileEvents = dirIndex >=0?new ArrayList<>(pollEvents.size()):null;

            boolean action = false;
            for(final WatchEvent<?> event:pollEvents){

              final Kind<?> kind = event.kind();

              if(kind == OVERFLOW){
                continue;
              }

              @SuppressWarnings("unchecked")
              final
              WatchEvent<Path> e =(WatchEvent<Path>)event;
              final Path fileName=e.context();

              final Path p = path.resolve(fileName);
              final int fileIndex = binarySearch(fileList, p);
              final FileWatchEvent fwe = fileIndex>=0 || dirIndex>=0?new FileWatchEvent(FileWatcher.this,p, kind):null;


              if(fileIndex>=0){
                action = true;
                final Key key = fileList.get(fileIndex);
                key.fireEvent(fwe);
              }

              if(dirIndex>=0){
                fileEvents.add(fwe);
              }
            }
            if(dirIndex>=0){
              action = true;
              final Key key = dirList.get(dirIndex);
              final DirectoryWatchEvent ev = new DirectoryWatchEvent(FileWatcher.this,path, fileEvents);
              key.fireEvent(ev);
            }

            if(!action){
              final int index = -binarySearch(fileList,path)-1;
              if(index >=0){
                final int s = fileList.size();
                for(int i=index;i<s;i++){
                  final Key key = fileList.get(i);
                  final Path p = key.getKey().getParent();
                  if(p.equals(path)){
                    action = true;
                    break;
                  }else if(!p.startsWith(path)){
                    break;
                  }
                }
              }
            }

            if(!action){
              //監視するものがいなくなった
              take.cancel();
            }

          });

          try {
            f.get();
          } catch (final ExecutionException e) {
            e.printStackTrace();
          }


          final boolean v = take.reset();
          if(!v){
            take.cancel();
          }
        }
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }
      running=false;
    };
    thread = new Thread(run, "watch wait thread");
    thread.setDaemon(true);
    thread.start();
  }


  public void stopWatch(){
    running = false;
    thread.interrupt();
  }

  private void addFile(final Consumer<PathWatchEvent> c,final Path path) throws IOException{
    final int index = binarySearch(fileList, path);
    if(index >=0){
      final Key k = fileList.get(index);
      k.add(c);
    }else{
      path.getParent().register(watcher, KINDS);
      final Key k =new Key(path, WatchType.FILE);
      k.add(c);
      fileList.add(-index-1, k);
    }
  }

  private void addDirectory(final Consumer<PathWatchEvent> c,final Path path) throws IOException{
    final int index = binarySearch(dirList, path);
    if(index >=0){
      final Key k = dirList.get(index);
      k.add(c);
    }else{
      path.register(watcher, KINDS);
      final Key k =new Key(path, WatchType.FILE);
      k.add(c);
      dirList.add(-index-1, k);
    }
  }

  private void removeFile(final Consumer<PathWatchEvent> c,final Path path){
    final int index = binarySearch(fileList, path);
    if(index >=0){
      final Key k = fileList.get(index);
      k.remove(c);
      if(k.isEmpty()){
        fileList.remove(index);
      }
    }
  }

  private void removeDirectory(final Consumer<PathWatchEvent> c,final Path path){
    final int index = binarySearch(dirList, path);
    if(index >=0){
      final Key k = dirList.get(index);
      k.remove(c);
      if(k.isEmpty()){
        dirList.remove(index);
      }
    }
  }


  public void watchFile(final Path path,final Consumer<PathWatchEvent> c){
    if(path == null || c == null) {
      return;
    }
    if(isDirectory(path)){
      return;
    }

    final Path p = path.normalize().toAbsolutePath();
    final Path dir = path.getParent();
    if(!exists(dir)){
      return;
    }

    if(isWatchThread()){
      try {
        addFile(c, path);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }else{
      final ThrowableCatchRunnable r = new ThrowableCatchRunnable(()->addFile(c,p));
      submit(r);
    }
  }


  public void watchDirectory(final Path path,final Consumer<PathWatchEvent> c){
    if(path == null || c == null) {
      return;
    }
    if(!isDirectory(path)){
      return;
    }

    final Path p = path.normalize().toAbsolutePath();
    if(isWatchThread()){
      try {
        addDirectory(c, path);
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }else{
      final ThrowableCatchRunnable r = new ThrowableCatchRunnable(()->addDirectory(c,p));
      submit(r);
    }
  }


  public void unwatchFile(final Path path,final Consumer<PathWatchEvent> c){
    if(path == null || c == null) {
      return;
    }
    final Path p = path.normalize().toAbsolutePath();
    if(isWatchThread()){
      removeFile(c, path);
    }else{
      submit(()->removeFile(c, p));
    }
  }

  public void unwatchDirectory(final Path path,final Consumer<PathWatchEvent> c){
    if(path == null || c == null) {
      return;
    }
    final Path p = path.normalize().toAbsolutePath();
    if(isWatchThread()){
      removeDirectory(c, path);
    }else{
      submit(()->removeDirectory(c, p));
    }
  }




  protected static class Key implements Comparable<Path>{
    private final Path key;
    private final WatchType type;
    private Actions action = EmptyActions;

    protected Key(final Path key,final WatchType type){
      this.key = requireNonNull(key,"key");
      this.type = requireNonNull(type);
    }

    protected final Path getKey(){
      return key;
    }

    protected final WatchType getType(){
      return type;
    }

    @Override
    public int compareTo(final Path o){
      return key.compareTo(o);
    }
    public void add(final Consumer<PathWatchEvent> c){action = action.add(c);}
    public void remove(final Consumer<PathWatchEvent> c){action = action.remove(c);}
    public boolean isEmpty(){return action.isEmpty(); }
    public void fireEvent(final PathWatchEvent e){action.fireEvent(e);}
  }

  protected static interface Actions{
    public Actions add(Consumer<PathWatchEvent> action);
    public Actions remove(Consumer<PathWatchEvent> action);
    public void fireEvent(PathWatchEvent e);
    default public boolean isEmpty(){return false;}
  }

  private static final Actions EmptyActions = new Actions(){
    @Override public Actions add(final Consumer<PathWatchEvent> action){
      return action == null? this: new SingleActions(action);
    }
    @Override public Actions remove(final Consumer<PathWatchEvent> action){return this;}
    @Override public void fireEvent(final PathWatchEvent e){}
    @Override public boolean isEmpty(){return true;}
  };

  private static final class SingleActions implements Actions{
    private final Consumer<PathWatchEvent> c;
    public SingleActions(final Consumer<PathWatchEvent> c){
      this.c = requireNonNull(c,"c");
    }
    @Override
    public Actions add(final Consumer<PathWatchEvent> action){
      if(c == action){
        return this;
      }
      return new MultiActions(c, action);
    }

    @Override
    public Actions remove(final Consumer<PathWatchEvent> action){
      if(c == action){
        return EmptyActions;
      }
      return this;
    }

    @Override
    public void fireEvent(final PathWatchEvent e){
      c.accept(e);
    }
  }

  private static final class MultiActions implements Actions{
    private Consumer<?>[] array;
    private int size;
    private boolean lock=false;
    public MultiActions(final Consumer<PathWatchEvent> a,final Consumer<PathWatchEvent> b){
      array = new Consumer[]{a,b};
      size = 2;
    }

    @Override
    public Actions add(final Consumer<PathWatchEvent> action){
      if(array.length > size){
        array[size++] = action;
      }else{
        final Consumer<?>[] newa = Arrays.copyOf(array, size+1);
        newa[size++] = action;
        array = newa;
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Actions remove(final Consumer<PathWatchEvent> action){
      int index = -1;
      final Consumer<?>[] arr = array;
      int size = this.size;
      for(int i=0;i<size;i++){
        if(arr[i]==action){
          index = i;
          break;
        }
      }
      if(index == -1){
        return this;
      }

      size--;
      if(size == 1){
        return new SingleActions((Consumer<PathWatchEvent>) arr[1-index]);
      }

      if(lock){
        final Consumer<?>[] newarr = new Consumer<?>[size];
        if(index!=0){
          System.arraycopy(arr, 0, newarr, 0, index);
        }
        if(index != size+1){
          System.arraycopy(arr, index+1, newarr, index, size-index);
        }

      }else{
        if(index!=0){
          System.arraycopy(arr, 0, arr, 0, index);
        }
        if(index != size){
          System.arraycopy(arr, index+1, arr, index, size-index);
        }

        arr[size] = null;
      }
      this.size = size;

      return this;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void fireEvent(final PathWatchEvent e){
      try{
        lock = true;
        final Consumer<?>[] array = this.array;
        final int size = this.size;

        for(int i=0;i<size;i++){
          final Consumer<PathWatchEvent> consumer = (Consumer<PathWatchEvent>) array[i];
          if(consumer!=null){
            consumer.accept(e);
          }
        }
      }finally{
        lock = false;
      }

    }
  }


  protected static Future<?> submit(final Runnable r){
    return WatchThread.THREAD.submit(r);
  }

  protected static <V> Future<V> submit(final Callable<V> c){
    return WatchThread.THREAD.submit(c);
  }
  protected static boolean isWatchThread(){
    return Thread.currentThread().getThreadGroup()==WatchThread.GROUP;
  }
  private static final class WatchThread{
    private static final ThreadGroup GROUP=new ThreadGroup("File watch thread");
    private static final ExecutorService THREAD;
    static{
      GROUP.setDaemon(true);
      final ExecutorService e = Executors.newSingleThreadExecutor(r->{
        final Thread t = new Thread(GROUP, r);
        t.setDaemon(true);
        return t;
      });
      THREAD=e;
    }
  }

  public static FileWatcher getDefault(){
    return A.watcher;
  }

  private static class A{
    static final FileWatcher watcher;
    static{
      FileWatcher w = null;
      try {
        w = new FileWatcher();
      } catch (final IOException e) {
      }
      watcher = w;
    }
  }


}
