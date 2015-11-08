package nodamushi.config;

import static java.util.Objects.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import nodamushi.config.prop.NProperties;

/**
 *
 * Preferencesはレジストリ汚すので使いたくないけど、
 * 自作のファイルPreferences作るほどの体力はないので作られたクラス。<br/>
 * 実行クラス(またはjar)のディレクトリ位置を取得して、そこにファイルを作る
 * テスト環境はWindowsだけ。。。
 * @author nodamushi
 *
 */
public class Settings{

  private Path basePath;
  private NProperties props=new NProperties();

  public Settings(){
    this(Settings.class);
  }

  public Settings(final Class<?> baseDirectoryClass){
    this(getApplicationDirectory(baseDirectoryClass)
        .orElseGet(()->Paths.get(".").toAbsolutePath()));
  }

  public Settings(final Path basePath){
    this.basePath=requireNonNull(basePath,"basePath");
    props.put("app.directory", basePath.toString());
  }

  public Path getBasePath(){return basePath;}

  public void setBasePath(Path p){
    if(p==null) {
      p = Paths.get(".").toAbsolutePath();
    }
    basePath = p;
    props.put("app.directory", basePath.toString());
  }

  public NProperties getProperty(){
    return props;
  }

  public void readProperty(final String fileName){
    final Path p = basePath.resolve(fileName);
    try {
      if(Files.isReadable(p)){
        props.read(p);
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }

  }








  private static Path remove(final Path source,final Path remove){

    if(source.endsWith(remove)){
      Path p=source.subpath(0, source.getNameCount()-remove.getNameCount());
      if(source.getRoot()!=null && p.getRoot()==null){
        p = source.getRoot().resolve(p);
      }
      return p;
    }
    return source;
  }

  public static Optional<Path> getApplicationDirectory(Class<?> claz){
    if(claz == null){
      claz = Settings.class;
    }

    Path p = null;
    String clname = claz.getName();
    final String packageName =claz.getPackage().getName();
    if(!packageName.isEmpty()){
      clname = clname.substring(packageName.length()+1);
    }
    clname +=".class";

    final URL clfile= claz.getResource(claz.getSimpleName()+".class");
    final String protocol = clfile.getProtocol();
    final Path pacPath = packageName.isEmpty()?null:Paths.get(packageName.replace('.', '/'));
    if(protocol.equals("file")){
      try {
        final Path path = Paths.get(clfile.toURI()).getParent();
        p =remove(path, pacPath);
      } catch (final URISyntaxException e) {

      }
    }else if(protocol.equals("jar")){
      try {
        final URL url = new URL(clfile.getPath());
        final Path path =Paths.get(url.toURI()).getParent();
        p =remove(path, pacPath).getParent();
      } catch (final MalformedURLException e) {
        try{
          final Path path = Paths.get(clfile.getPath()).getParent();
          p =remove(path, pacPath).getParent();
        }catch(final InvalidPathException e2){

        }
      } catch (final URISyntaxException e) {
      }
    }

    return Optional.ofNullable(p);

  }








}
