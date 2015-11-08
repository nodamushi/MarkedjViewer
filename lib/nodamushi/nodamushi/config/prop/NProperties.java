package nodamushi.config.prop;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodamushi.nio.FileUtil;

public class NProperties{

  private Map<String, String> map;

  public NProperties(){
    map = new HashMap<>();
  }

  private static final Pattern PATTERN=Pattern.compile("\\$\\(([^\r\n\u0085\u2028\u2029()=\\s/$]+)\\)");

  public void put(final String key,final String value){
    map.put(key, value);
  }

  public void delete(final String key){
    map.remove(key);
  }

  public String _get(final String key){
    final String s = map.get(key);
    if(s == null) {
      return null;
    }
    final Matcher m = PATTERN.matcher(s);
    int index=0;
    final StringBuilder sb = new StringBuilder();
    final int size = s.length();
    boolean find = false;
    while(index < size && m.find(index)){
      find = true;
      final int start = m.start();
      final int end = m.end();
      final String ss=_get(m.group(1));
      if(start!=index){
        sb.append(s.substring(index, start));
      }
      if(ss!=null){
        sb.append(ss);
      }
      index = end;
    }
    if(!find){
      return map.get(key);
    }
    if(index < size){
      sb.append(s.substring(index));
    }

    return sb.toString();
  }

  public Optional<String> get(final String key){
    return Optional.ofNullable(_get(key));
  }

  public Optional<Integer> getAsInt(final String key){
    final String s = _get(key);
    try{
      if(s != null){
        return Optional.ofNullable(Integer.parseInt(s));
      }
    }catch(final Exception e){}
    return Optional.ofNullable(null);
  }

  public Optional<Double> getAsDouble(final String key){
    final String s = _get(key);
    try{
      if(s != null) {
        return Optional.ofNullable(Double.parseDouble(s));
      }
    }catch(final Exception e){}
    return Optional.ofNullable(null);
  }

  public Optional<Path> getAsPath(final String key){
    final String s = _get(key);
    try{
      if(s != null) {
        return Optional.ofNullable(Paths.get(s));
      }
    }catch(final Exception e){}
    return Optional.ofNullable(null);
  }

  public void read(final Path p)throws IOException{
    read(p,null);
  }

  public void read(final Path p,Charset c)throws IOException{
    if(c == null){
      c =FileUtil.getCharset(p);
    }

    try(Reader r=Files.newBufferedReader(p, c)){
      read(r);
    }
  }

  public void read(final Reader r)throws IOException{
    final Lexer l = new Lexer(r);
    Token t;
    Token key=null;
    while((t = l.lex())!=null){
      if(t.isArg()){
        map.put(key.getValue(), t.getValue());
        key = null;
      }else{
        if(key!=null){
          map.put(key.getValue(),"");
        }
        key = t;
      }
    }

    if(key!=null){
      map.put(key.getValue(),"");
    }

  }


}
