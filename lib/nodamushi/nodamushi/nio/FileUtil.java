package nodamushi.nio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.universalchardet.UniversalDetector;



public class FileUtil{


  private static final Pattern LINE_SEPARATOR_PATTERN=Pattern.compile("\r\n|[\n\r\u2028\u2029\u0085\u000c]");
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");


  public static void writeText(final String text,final String path)throws IOException{
    writeText(text, (Charset)null, LINE_SEPARATOR, Paths.get(path));
  }

  public static void writeText(final String text,final Path path)throws IOException{
    writeText(text, (Charset)null, LINE_SEPARATOR, path);
  }


  public static void writeText(final String text,final String charset,final String path)throws IOException{
    writeText(text, charset==null?null:Charset.forName(charset), LINE_SEPARATOR, Paths.get(path));
  }

  public static void writeText(final String text,final String charset,final Path path)throws IOException{
    writeText(text, charset==null?null:Charset.forName(charset), LINE_SEPARATOR, path);
  }

  public static void writeText(final String text,final String charset,final String lineTerminator,final String path)throws IOException{
    writeText(text, charset==null?null:Charset.forName(charset), lineTerminator, Paths.get(path));
  }

  public static void writeText(final String text,final String charset,final String lineTerminator,final Path path)throws IOException{
    writeText(text, charset==null?null:Charset.forName(charset), lineTerminator, path);
  }



  public static void writeText(final String text,final Charset c,final String lineTerminator,final String path)throws IOException{
    writeText(text, c, lineTerminator, Paths.get(path));
  }

  public static void writeText(String text,Charset c,String lineTerminator,final Path p)throws IOException{
    if(lineTerminator == null){
      lineTerminator = LINE_SEPARATOR;
    }
    if(c == null){
      c = Charset.defaultCharset();
    }
    if(text == null){
      text = "";
    }
    try(Writer w = Files.newBufferedWriter(p, c)){
      _writeText(text, c, lineTerminator, w);
    }
  }

  private static void _writeText(final String text,final Charset c,final String lineTerminator,final Writer w)throws IOException{
    final Matcher m = LINE_SEPARATOR_PATTERN.matcher(text);
    int index = 0;
    final int l = text.length();
    final boolean lt = !lineTerminator.isEmpty();
    while(m.find(index)){
      final int s = m.start();
      final int e = m.end();
      final int ll = s-index;
      if(ll!=0){
        w.write(text, index, ll);
      }

      if(lt){
        w.write(lineTerminator);
      }

      index = e;
      if(index == l){
        break;
      }
    }

    if(index != l){
      w.write(text, index, l-index);
    }
  }



  public static String readText(final Path path)throws IOException{
    return readText(path,null);
  }

  public static String readText(final String path,final String charset) throws IOException{
    final Charset c = charset==null?null:Charset.forName(charset);
    return readText(Paths.get(path),c);
  }

  public static String readText(final String path)throws IOException{
    return readText(path,null);
  }

  public static  String readText(final Path path,Charset c)throws IOException{
    if(c!=null){
      return _readText(path, c);
    }

    try(InputStream in = new BufferedInputStream(Files.newInputStream(path))){
      final int bsize = 1000;
      final byte[] buf = new byte[bsize];

      final UniversalDetector ud = new UniversalDetector(null);

      int read;
      while(!ud.isDone() && (read = in.read(buf))>0){
        ud.handleData(buf, 0, read);
      }

      ud.dataEnd();
      final String encoding = ud.getDetectedCharset();
      if(encoding != null){
        c = Charset.forName(encoding);
      }
    }
    if(c==null){
      c = Charset.defaultCharset();
    }
    return _readText(path, c);
  }

  public static Charset getCharset(final Path path){
    Charset c = null;
    try(InputStream in = new BufferedInputStream(Files.newInputStream(path))){
      final int bsize = 1000;
      final byte[] buf = new byte[bsize];

      final UniversalDetector ud = new UniversalDetector(null);

      int read;
      while(!ud.isDone() && (read = in.read(buf))>0){
        ud.handleData(buf, 0, read);
      }

      ud.dataEnd();
      final String encoding = ud.getDetectedCharset();
      if(encoding != null){
        c = Charset.forName(encoding);
      }
    } catch (final IOException e) {

    }
    if(c==null){
      c = Charset.defaultCharset();
    }
    return c;
  }

  private static String _readText(final Path path,final Charset c) throws IOException{
    final StringBuilder sb = new StringBuilder();
    try(Reader r = Files.newBufferedReader(path,c)){
      final int bsize= 1000;
      final char[] buf=new char[bsize];
      int read;
      if(c.name().startsWith("UTF-")){
        while(true){
          read = r.read(buf);
          if(read ==-1){
            return "";
          }
          if(read > 0){
            final int s = buf[0] == 65279?1:0;//BOM
            sb.append(buf,s,read-s);
            break;
          }
        }
      }
      while((read = r.read(buf))!=-1){
        sb.append(buf,0,read);
      }
    }
    return sb.toString();
  }

  private FileUtil(){}
}
