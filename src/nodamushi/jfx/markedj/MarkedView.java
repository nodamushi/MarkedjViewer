package nodamushi.jfx.markedj;



import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker.State;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MarkedView extends Region{

  public static enum ReloadFramePosition{
    ORIGIN,
    FIX_TOP(0,1),FIX_TOP_LEFT(1,1),FIX_TOP_RIGHT(-1,1),
    FIX_BOTTOM(0,-1),FIX_BOTTOM_LEFT(1,-1),FIX_BOTTOM_RIGHT(-1,-1),
    FIX_LEFT(1,0),FIX_RIGHT(-1,0)

    ;
    public final boolean origin;
    public final boolean fixTop;
    public final boolean fixBottom;
    public final boolean fixLeft;
    public final boolean fixRight;
    private ReloadFramePosition(){
      origin=true;
      fixBottom=fixLeft=fixTop=fixRight=false;
    }
    private ReloadFramePosition(final int v,final int h){
      origin=v==0 && h==0;
      fixTop = h==1;
      fixBottom = h==-1;
      fixLeft = v==1;
      fixRight=v==-1;
    }

  }
  private static PseudoClass EMPTY=PseudoClass.getPseudoClass("empty");

  private WebView view;
  private WebEngine engine;
  private Label emptyMessage;

  public MarkedView(){
    getStyleClass().setAll("md-view");
    view = new WebView();
    emptyMessage = new Label("Drag & Drop Markdown file");
    emptyMessage.getStyleClass().setAll("empty-message");
    getChildren().addAll(emptyMessage,view);
    engine = view.getEngine();
    update(null);
    pseudoClassStateChanged(EMPTY, true);
    engine.getLoadWorker().stateProperty().addListener(o->{
      if(engine.getLoadWorker().getState()==State.SUCCEEDED){
        setScrollPosition();
      }
    });
  }

  public void reload(){
    update(null);
  }


  @Override
  protected void layoutChildren(){
    layoutInArea(emptyMessage, 0, 0, getWidth(), getHeight(), 0,
        getInsets(), false, false, HPos.CENTER , VPos.CENTER);
    layoutInArea(view, 0, 0, getWidth(), getHeight(), 0,
        getInsets(), true, true, HPos.LEFT , VPos.TOP);
  }
  @Override
  protected double computePrefHeight(final double width){
    return 600;
  }

  @Override
  protected double computePrefWidth(final double height){
    return 800;
  }

  public WebEngine getEngine(){
    return engine;
  }

  private static final String DEFAULT_HTML="<!DOCTYPE html><html><body>Open file</body></html>";


  private InvalidationListener update = this::update;
  private void update(final Observable o){
    final MarkedHTML markedHtml = getMarkedHtml();
    scrollX=Double.NaN;
    scrollY=Double.NaN;
    final boolean b=markedHtml!=null && markedHtml.isChangeURI();
    if(!b){
      getScrollPosition();
    }
    String html;
    if(markedHtml==null){
      html = DEFAULT_HTML;
    }else{
      html = markedHtml.getHtml();
    }


    engine.loadContent(html);
    pseudoClassStateChanged(EMPTY,markedHtml!=null? markedHtml.getReader()==null:true);
    if(!b) {
      setScrollPosition();
    }
  }


  private static final String GET_SCROLL_POSITION="["
      + "(document.documentElement.scrollLeft|document.body.scrollLeft),"
      + "(document.documentElement.scrollTop|document.body.scrollTop)"
      + "];";

  private static final String GET_LEFT_POSITION="(document.documentElement.scrollLeft|document.body.scrollLeft);";
  private static final String GET_RIGHT_POSITION=
      "Math.max(document.body.clientWidth|0 , document.body.scrollWidth|0, document.documentElement.scrollWidth|0, document.documentElement.clientWidth|0)"
      + "-(document.documentElement.scrollLeft|document.body.scrollLeft);";
  private static final String GET_TOP_POSITION="(document.documentElement.scrollTop|document.body.scrollTop);";
  private static final String GET_BOTTOM_POSITION=""
      + "Math.max(document.body.clientHeight|0 , document.body.scrollHeight|0, document.documentElement.scrollHeight|0, document.documentElement.clientHeight|0)"
      + "-(document.documentElement.scrollTop|document.body.scrollTop);";

  private double getPosValue(final String code){
    return ((Number)engine.executeScript(code)).doubleValue();
  }

  private ReloadFramePosition dataPosition=ReloadFramePosition.ORIGIN;
  private double scrollX=Double.NaN,scrollY=Double.NaN;
  private void getScrollPosition(){
    ReloadFramePosition p = getReloadFramePosition();
    if(p==null) {
      p=ReloadFramePosition.ORIGIN;
    }
    dataPosition = p;

    if(p.fixLeft){
      scrollX = getPosValue(GET_LEFT_POSITION);
    }else if(p.fixRight){
      scrollX = getPosValue(GET_RIGHT_POSITION);
    }

    if(p.fixTop){
      scrollY = getPosValue(GET_TOP_POSITION);
    }else if(p.fixBottom){
      scrollY = getPosValue(GET_BOTTOM_POSITION);
    }


  }

  private void setScrollPosition(){
    if(dataPosition == ReloadFramePosition.ORIGIN || (scrollX!=scrollX && scrollY!=scrollY)) {
      return;
    }
    String x="0",y="0";
    final ReloadFramePosition p = dataPosition;

    if(p.fixLeft){
      x = String.valueOf(scrollX);
    }else if(p.fixRight){
      x = "Math.max(document.body.clientWidth|0 , document.body.scrollWidth|0, document.documentElement.scrollWidth|0, document.documentElement.clientWidth|0)-"
          +scrollX;
    }

    if(p.fixTop){
      y = String.valueOf(scrollY);
    }else if(p.fixBottom){
      y = "Math.max(document.body.clientHeight|0 , document.body.scrollHeight|0, document.documentElement.scrollHeight|0, document.documentElement.clientHeight|0)-"
          +scrollY;
    }

    final String s = "window.scroll("+x+","+y+");";
    engine.executeScript(s);
  }


  /**
   *
   * @return
   */
  public final ObjectProperty<MarkedHTML> markeHtmlProperty(){
    if (markeHtmlProperty == null) {
      markeHtmlProperty = new SimpleObjectProperty<>(this, "markeHtml", null);
      markeHtmlProperty.addListener((ob,o,n)->{
        if(o!=null){
          o.htmlProperty().removeListener(update);
        }
        if(n!=null){
          n.htmlProperty().addListener(update);
        }
        update(ob);
      });

    }
    return markeHtmlProperty;
  }

  public final MarkedHTML getMarkedHtml(){
    return markeHtmlProperty == null ? null : markeHtmlProperty.get();
  }

  public final void setMarkedHtml(final MarkedHTML value){
    markeHtmlProperty().set(value);
  }

  private ObjectProperty<MarkedHTML> markeHtmlProperty;





  /**
   *
   * @return
   */
  public final ObjectProperty<ReloadFramePosition> reloadFramePositionProperty(){
    if (reloadFramePositionProperty == null) {
      reloadFramePositionProperty = new SimpleObjectProperty<>(this, "reloadFramePosition", ReloadFramePosition.FIX_BOTTOM_LEFT);
    }
    return reloadFramePositionProperty;
  }

  public final ReloadFramePosition getReloadFramePosition(){
    return reloadFramePositionProperty == null ? ReloadFramePosition.FIX_BOTTOM_LEFT : reloadFramePositionProperty.get();
  }

  public final void setReloadFramePosition(final ReloadFramePosition value){
    reloadFramePositionProperty().set(value);
  }

  private ObjectProperty<ReloadFramePosition> reloadFramePositionProperty;

}
