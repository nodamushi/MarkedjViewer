package nodamushi.jfx.stage;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Nodeが属するWindowの状態で、Nodeの疑似クラスの状態を変化させるRegion<br/><br/>
 * 疑似クラス<br/>
 * <ul>
 * <li>wfocused:WindowがFocusを持っているかどうか
 * <li>maximize:Stageが最大化しているかどうか
 * <li>full:StageがFullScreenかどうか
 * </ul>
 * @author nodamushi
 *
 */
public class WindowRegion extends Region{

  /**StageがMaximizeかどうか*/
  protected static final PseudoClass MAXIMIZE_PSEUDO_CLASS=PseudoClass.getPseudoClass("maximize");
  /**StageがFullScreenかどうか*/
  protected static final PseudoClass FULLSCREEN_PSEUDO_CLASS=PseudoClass.getPseudoClass("full");
  /**WindowがFocusを持っているかどうか*/
  protected static final PseudoClass WINDOW_FOCUS_PSEUDO_CLASS=PseudoClass.getPseudoClass("wfocused");


  public WindowRegion(){
    sceneProperty().addListener((ob,o,n)->{
      if(o!=null){
        o.windowProperty().removeListener(changeStage);
      }
      if(n!=null){
        n.windowProperty().addListener(changeStage);
        setOwnerStage(n.getWindow());
      }else{
        setOwnerStage(null);
      }
    });
  }

  //----------------------------------------------------------------------------------


  private void setOwnerStage(final Window w){
    final Stage ownerStage = _getOwnerStage();
    final Window ownerWindow = _getOwnerWindow();
    if(ownerWindow!=null){
      ownerWindow.showingProperty().removeListener(changeWindowShowing);
      ownerWindow.focusedProperty().removeListener(changeWindowFocus);
      if(ownerStage!=null){
        final Stage s = ownerStage;
        s.maximizedProperty().removeListener(changeStageMaximize);
        s.fullScreenProperty().removeListener(changeFullScreen);
      }
    }

    final Window newownerWindow=w;
    final Stage newownerStage =(w instanceof Stage)?(Stage)w: null;
    this.ownerWindow = newownerWindow;
    this.ownerStage = newownerStage;

    if(w!=null){
      w.showingProperty().addListener(changeWindowShowing);
      w.focusedProperty().addListener(changeWindowFocus);
      if(newownerStage!=null){
        newownerStage.maximizedProperty().addListener(changeStageMaximize);
        newownerStage.fullScreenProperty().addListener(changeFullScreen);
      }
    }
    ownerWindowProperty.fireValueChangedEvent();
    ownerStageProperty.fireValueChangedEvent();
    changeWindowShowing(null);
    changeWindowFocus(null);
    changeStageMaximize(null);
    changeFullScreen(null);
  }
  private final ChangeListener<Window> changeStage = (o, old,newWindow)->{
    setOwnerStage(newWindow);
  };
  private final InvalidationListener changeStageMaximize = this::changeStageMaximize;
  private void changeStageMaximize(final Observable o){
    final boolean stageMaximized = ownerStage==null?false:ownerStage.isMaximized();
    stageMaximizedWrapper.set(stageMaximized);
    pseudoClassStateChanged(MAXIMIZE_PSEUDO_CLASS, stageMaximized);
  }

  private final InvalidationListener changeFullScreen = this::changeFullScreen;
  private void changeFullScreen(final Observable o){
    final boolean fullscreen =ownerStage==null?false: ownerStage.isFullScreen();
    fullScreenWrapper.set(fullscreen);
    pseudoClassStateChanged(FULLSCREEN_PSEUDO_CLASS, fullscreen);
  }
  private final InvalidationListener changeWindowFocus = this::changeWindowFocus;
  private void changeWindowFocus(final Observable o){
    final boolean windowFocused =ownerWindow==null?false: ownerWindow.isFocused();
    windowFocusedWrapper.set(windowFocused);
    pseudoClassStateChanged(WINDOW_FOCUS_PSEUDO_CLASS, windowFocused);
  }
  private final InvalidationListener changeWindowShowing = this::changeWindowShowing;
  private void changeWindowShowing(final Observable o){
    final boolean windowShowing = ownerWindow==null?false :ownerWindow.isShowing();
    windowShowingWrapper.set(windowShowing);
  }


  //----------------------------------------------------------------------------------
  //Window API

  /**
   * このNodeが属するWindowがStageであるかどうか
   * @return
   */
  public boolean isOwnerStage(){
    ownerWindowProperty.validate= ownerStageProperty.validate = true;
    return ownerStage != null;
  }
  /**
   * ownerWindowProperty,ownerStagePropertyのvalidateを変化させずに、
   * isOwnerStageの結果を返す
   * @return
   */
  protected boolean _isOwnerStage(){
    return ownerStage != null;
  }

  /**
   * このNodeが属するWindowが存在するかどうか
   * @return
   */
  public boolean hasOwnerWindow(){
    ownerWindowProperty.validate = true;
    return ownerWindow != null;
  }

  /**
   * ownerWindowPropertyのvalidateを変化させずに、
   * hasOwnerWindowの結果を返す
   * @return
   */
  protected boolean _hasOwnerWindow(){
    return ownerWindow != null;
  }

  private double pressX,pressY,pressWindowX,pressWindowY;
  /**
   * windowMoveActionでMouseEventをconsumeするかどうか。(default is true)<br/>
   * 対象はMOUSE_PRESSED,MOUSE_DRAGGED,MOUSE_RELEASED
   */
  protected boolean consumeEventInWindowMoveAction=true;

  /**
   * マウスイベントによりウィンドウを移動させるAPI
   *
   * @param e
   * @see WindowRegion#consumeEventInWindowMoveAction
   */
  protected void windowMoveAction(final MouseEvent e){
    if(ownerWindow==null) {
      return;
    }
    final EventType<? extends MouseEvent> t = e.getEventType();

    if(t == MouseEvent.MOUSE_PRESSED){
      pressX = e.getScreenX();
      pressY = e.getScreenY();
      pressWindowX = ownerWindow.getX();
      pressWindowY = ownerWindow.getY();
      if(consumeEventInWindowMoveAction) {
        e.consume();
      }
    }else if(t == MouseEvent.MOUSE_DRAGGED){
      final double x = e.getScreenX();
      final double y = e.getScreenY();
      final double movx = x-pressX;
      final double movy = y-pressY;
      final double winx = pressWindowX+movx;
      final double winy = pressWindowY+movy;
      ownerWindow.setX(winx);
      ownerWindow.setY(winy);
      if(consumeEventInWindowMoveAction) {
        e.consume();
      }
    }else if(t == MouseEvent.MOUSE_RELEASED){
      if(consumeEventInWindowMoveAction) {
        e.consume();
      }
    }
  }



  /**
   * ownerWindowを隠す
   */
  protected void hideWindow(){
    if(ownerWindow==null) {
      return;
    }
    ownerWindow.hide();
  }
  /**
   * ownerStageのshowを呼び出す
   */
  protected void showStage(){
    if(ownerStage==null) {
      return;
    }
    ownerStage.show();
  }
  /**
   * ownerWindowがPopupWindowであるとき、{@link PopupWindow#show(Window)}を呼び出す
   * @param owner
   */
  protected void popupWindow(final Window owner){
    if(ownerWindow instanceof PopupWindow){
      ((PopupWindow)ownerWindow).show(owner);
    }
  }
  /**
   * ownerStageのsetFullScreenにvalueを渡す
   * @param value
   */
  protected void setFullScreen(final boolean value){
    if(ownerStage==null) {
      return;
    }
    ownerStage.setFullScreen(value);
  }

  /**
   * ownerStageのsetMaximizedにvalueを渡す
   * @param value
   */
  protected void setMaximizedStage(final boolean value){
    if(ownerStage==null) {
      return;
    }
    ownerStage.setMaximized(value);
  }









  //-------------------------Property----------------------------------------




  /**
   * このNodeが属するWindowがフォーカスを持っているかどうか
   * @return
   */
  public final ReadOnlyBooleanProperty windowFocusedProperty(){
    return windowFocusedWrapper.getReadOnlyProperty();
  }

  public final boolean isWindowFocused(){
    return windowFocusedWrapper.get();
  }

  private final ReadOnlyBooleanWrapper windowFocusedWrapper = new ReadOnlyBooleanWrapper(this, "windowFocused", false);



  /**
   * このNodeが属するStageがFull Screenで表示されているかどうか。<br/>
   * このNodeが属するWindowがStageでない場合はfalse
   * @return
   */
  public final ReadOnlyBooleanProperty fullScreenProperty(){
    return fullScreenWrapper.getReadOnlyProperty();
  }

  public final boolean isFullScreen(){
    return fullScreenWrapper.get();
  }

  private final ReadOnlyBooleanWrapper fullScreenWrapper = new ReadOnlyBooleanWrapper(this, "fullScreen", false);


  /**
   *
   * @return
   */
  public final ReadOnlyBooleanProperty windowShowingProperty(){
    return windowShowingWrapper.getReadOnlyProperty();
  }

  public final boolean isWindowShowing(){
    return windowShowingWrapper.get();
  }


  private final ReadOnlyBooleanWrapper windowShowingWrapper = new ReadOnlyBooleanWrapper(this, "windowShowing", false);



  /**
   * このNodeが属するStageが最大化されているかどうか。<br/>
   * このNodeが属するWindowがStageでない場合はfalse
   * @return
   */
  public final ReadOnlyBooleanProperty stageMaximizedProperty(){
    return stageMaximizedWrapper.getReadOnlyProperty();
  }

  public final boolean isStageMaximized(){
    return stageMaximizedWrapper.get();
  }

  private final ReadOnlyBooleanWrapper stageMaximizedWrapper = new ReadOnlyBooleanWrapper(this, "stageMaximized", false);

  private Window ownerWindow;
  private Stage ownerStage;
  private final ReadOnlyPropertyBase2<Window> ownerWindowProperty = new ReadOnlyPropertyBase2<Window>("ownerWindow"){
    @Override
    protected Window _get(){
      return ownerWindow;
    }
  };
  private final ReadOnlyPropertyBase2<Stage> ownerStageProperty = new ReadOnlyPropertyBase2<Stage>("ownerStage"){
    @Override
    protected Stage _get() {
      return ownerStage;
    }
  };
  /**
   * ownerStagePropertyのvalidate状態を変化させることなくownerStageを取得する
   * @return
   */
  protected final Window _getOwnerWindow(){
    return ownerWindow;
  }

  public final Window getOwnerWindow(){
    ownerWindowProperty.validate=true;
    return ownerWindow;
  }
  /**
   * このNodeが属するWindow
   * @return
   */
  public final ReadOnlyObjectProperty<Window> ownerWindowProperty(){
    return ownerWindowProperty;
  }
  /**
   * ownerStagePropertyのvalidate状態を変化させることなくownerStageを取得する
   * @return
   */
  protected final Stage _getOwnerStage(){
    return ownerStage;
  }

  public final Stage getOwnerStage(){
    ownerStageProperty.validate=true;
    return ownerStage;
  }
  /**
   * このNodeが属するStage<br/>
   * WindowがStageでない場合はnull
   * @return
   */
  public final ReadOnlyObjectProperty<Stage> ownerStageProperty(){
    return ownerStageProperty;
  }

  protected abstract class ReadOnlyPropertyBase2<T> extends ReadOnlyObjectPropertyBase<T>{
    private final String name;
    protected boolean validate=true;
    public ReadOnlyPropertyBase2(final String name){
      this.name = name==null?"":name;
    }
    @Override
    public final Object getBean(){
      return WindowRegion.this;
    }
    @Override
    public final String getName(){
      return name;
    }
    @Override
    public final T get(){
      validate = true;
      return _get();
    }
    protected abstract T _get();
    @Override
    public void fireValueChangedEvent(){
      if(validate){
        validate = false;
        super.fireValueChangedEvent();
      }
    }
  }

}
