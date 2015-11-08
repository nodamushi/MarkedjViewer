package nodamushi.jfx.util;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.KeyEvent;

public class KeyCombinations{
  @FunctionalInterface
  public static interface KeyAction{
    public void action(Scene s);
  }

  private static class Entry{
    private final KeyCombination kc;
    private final KeyAction action;
    public Entry(final KeyCombination kc,final KeyAction action){
      this.kc = requireNonNull(kc);
      this.action = requireNonNull(action);
    }

  }

  private List<Entry> list = new ArrayList<>();

  /**
   * autoConsumePropertyをtrue,execAllPropertyをfalseで初期します。
   */
  public KeyCombinations(){
    this(true,false);
  }
  /**
   *
   * @param autoConsume autoConsumePropertyの初期値
   * @param execAll execAllPropertyの初期値
   */
  public KeyCombinations(final boolean autoConsume,final boolean execAll){
    _consumeAuto=autoConsume;
    _execAll = execAll;
  }


  /**
  *
  * @param kc
  * @param action
  * @return このインスタンス（チェイン用）
  */
 public KeyCombinations add(final KeyAction action,final KeyCombination kc){
   list.add(new Entry(kc, action));
   return this;
 }

  public KeyCombinations add(final KeyAction action,final KeyCode code,final Modifier... modify){
    final KeyCodeCombination kc =  new KeyCodeCombination(code, modify);
    list.add(new Entry(kc, action));
    return this;
  }

  final EventHandler<KeyEvent> eh = this::action;
  public void install(final Scene s){
    s.addEventFilter(KeyEvent.KEY_PRESSED , eh);
  }

  public void uninstall(final Scene s){
    s.removeEventFilter(KeyEvent.KEY_PRESSED,eh);
  }

  private void action(final KeyEvent event){
    final Scene scene = (Scene)event.getSource();
    boolean b=false;
    final boolean ea=!isExecAll();
    for(final Entry e:list){
      if(e.kc.match(event)){
        e.action.action(scene);
        b=true;
        if(ea) {
          break;
        }
      }
    }
    if(b && isAutoConsume()){
      event.consume();
    }
  }


  private final boolean _execAll;
  /**
   * 一つEntryにマッチした後もマッチするキーバインドがあれば、それらをすべて実行するかどうか。<br/>
   * falseの場合は最初にマッチしたEntryのみ実行する<br/>
   * @return
   */
  public final BooleanProperty execAllProperty(){
    if (execAllProperty == null) {
      execAllProperty = new SimpleBooleanProperty(this, "execAll", _execAll);
    }
    return execAllProperty;
  }

  public final boolean isExecAll(){
    return execAllProperty == null ? _execAll : execAllProperty.get();
  }

  public final void setExecAll(final boolean value){
    execAllProperty().set(value);
  }

  private BooleanProperty execAllProperty;


  private final boolean _consumeAuto;


  /**
   * KeyEventをconsumeするかどうか
   * @return
   */
  public final BooleanProperty autoConsumeProperty(){
    if (autoConsumeProperty == null) {
      autoConsumeProperty = new SimpleBooleanProperty(this, "autoConsume", _consumeAuto);
    }
    return autoConsumeProperty;
  }

  public final boolean isAutoConsume(){
    return autoConsumeProperty == null ? _consumeAuto : autoConsumeProperty.get();
  }

  public final void setAutoConsume(final boolean value){
    autoConsumeProperty().set(value);
  }

  private BooleanProperty autoConsumeProperty;



}
