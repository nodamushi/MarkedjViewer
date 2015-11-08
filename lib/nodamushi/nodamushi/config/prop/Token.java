package nodamushi.config.prop;

import static java.util.Objects.*;

public class Token{
  public static enum Type{
    KEY,ARG
  }


  private final int x,y;
  private final String value;
  private final Type type;

  public static Token keyToken(final String value,final int x,final int y){
    return new Token(value,x,y,Type.KEY);
  }

  public static Token argToken(final String value,final int x,final int y){
    return new Token(value,x,y,Type.ARG);
  }


  public Token(final String value,final int x,final int y,final Type type){
    this.x = x;
    this.y = y;
    this.value = value == null?"":value;
    this.type = requireNonNull(type,"type");
  }

  public int getX(){
    return x;
  }
  public int getY(){
    return y;
  }
  public Type getType(){
    return type;
  }
  public String getValue(){
    return value;
  }

  public boolean isKey(){
    return type ==Type.KEY;
  }

  public boolean isArg(){
    return type == Type.ARG;
  }
}
