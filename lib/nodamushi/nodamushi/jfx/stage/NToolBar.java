package nodamushi.jfx.stage;


import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.StyleConverter;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import nodamushi.jfx.stage.NToolBar.ShowState;


public class NToolBar extends WindowRegion{
  private static final Duration DEFAULT_TIME=Duration.millis(100);


  private final Timeline showAnime = new Timeline();
  private final Rectangle mask = new Rectangle();

  public NToolBar(){
    this(0,(Node[]) null);
  }

  public NToolBar(final double space){
    this(space,(Node[]) null);
  }

  public NToolBar(final double space,final Node... items){
    getStyleClass().setAll("n-tool-bar");
//    box = new HBox();
    setSpacing(space);
//    box.spacingProperty().bind(spacingProperty);
    if(items!=null && items.length!=0){
//      box.getChildren().addAll(items);
      getChildren().addAll(items);
    }
//    getChildren().add(box);
    showAnime.setCycleCount(1);
    showAnime.getKeyFrames().addAll(
        new KeyFrame(Duration.ZERO, new KeyValue(sizeProperty, 0)),
        new KeyFrame(DEFAULT_TIME, new KeyValue(sizeProperty, 1)));
    showAnime.jumpTo(DEFAULT_TIME);
    sizeProperty.addListener(this::requestLayout);
    windowFocusedProperty().addListener(this::checkFocus);
    windowShowingProperty().addListener(this::checkFocus);
    setClip(mask);
    mask.widthProperty().bind(widthProperty());
    mask.heightProperty().bind(heightProperty());
  }

  private void checkFocus(final Observable o){
    if(getShowState()!=ShowState.AUTO) {
      return;
    }
    if(isWindowFocused()){
      if(!isWindowShowing()) {
        showAnime.pause();
        return;
      }
      playOpenAnimation();
    }else{
      if(!isWindowShowing()) {
        showAnime.pause();
        return;
      }
      playHideAnimation();
    }
  }

  private void requestLayout(final Observable o){
    requestLayout();
  }

  protected void playHideAnimation(){
    if(!isAnimation()){
      showAnime.pause();
      showAnime.jumpTo(Duration.ZERO);
      sizeProperty.set(0);
      return;
    }
    if(sizeProperty.get()==0) {
      return;
    }
    if(showAnime.getStatus()==Status.RUNNING){
      if(showAnime.getRate()==-1) {
        return;
      }
      showAnime.pause();
    }
    showAnime.setRate(-1);
    showAnime.play();
  }

  protected void playOpenAnimation(){
    if(!isAnimation()){
      showAnime.pause();
      showAnime.jumpTo(getAnimationTime());
      sizeProperty.set(1);
      return;
    }
    if(sizeProperty.get()==1) {
      return;
    }

    if(showAnime.getStatus()==Status.RUNNING){
      if(showAnime.getRate()==1) {
        return;
      }
      showAnime.pause();
    }
    showAnime.setRate(1);
    showAnime.play();
  }


  @Override
  public ObservableList<Node> getChildren(){
    return super.getChildren();
  }

  private double marginHeight(){
    final Insets i = getInsets();
    return i.getTop()+i.getBottom();
  }
  private double marginWidth(){
    final Insets i = getInsets();
    return i.getRight()+i.getLeft();
  }

  private DoubleProperty sizeProperty = new SimpleDoubleProperty(1);

  private double getSize(){return sizeProperty.get();}

  @Override
  protected double computeMinHeight(final double width){
    return 0;
  }

  @Override
  protected double computeMinWidth(final double height){
    return 0;
  }

  @Override
  protected double computePrefHeight(final double width){

    if(isHorizontal()){
      double value = 0;
      for(final Node n:getChildren()){
        value = Math.max(value, n.prefHeight(-1));
      }
      return (value+marginHeight())*getSize();
    }else{
      final double space = getSpacing();
      double value = space*max(0, getChildren().size()-1);
      for(final Node n:getChildren()){
        value +=n.prefHeight(-1);
      }
      return value + marginHeight();
    }
  }

  @Override
  protected double computePrefWidth(final double height){
    if(isVertical()){
      double value = 0;
      for(final Node n:getChildren()){
        value = Math.max(value, n.prefWidth(-1));
      }
      return (value+marginWidth())*getSize();
    }else{
      final double space = getSpacing();
      double value = space*max(0, getChildren().size()-1);
      for(final Node n:getChildren()){
        value +=n.prefWidth(-1);
      }
      return value + marginWidth();
    }
  }


  @Override
  protected void layoutChildren(){
    final Insets i = getInsets();
    double x = 0;
    final double s = getSpacing();
    if(isHorizontal()){
      x = i.getLeft();
      final Insets ii = new Insets(i.getTop(), 0, i.getBottom(), 0);
      final double margin = marginHeight();
      final double h = getHeight();
      final double hm = h-margin;
      for(final Node n:getChildren()){
        final double w = n.prefWidth(hm);
        layoutInArea(n, x, 0, w, h, 0, ii, false, false, HPos.CENTER, VPos.CENTER, true);
        x += w+s;
      }

    }else{
      x = i.getTop();
      final Insets ii = new Insets(0,i.getRight(), 0, i.getLeft());
      final double margin = marginWidth();
      final double w = getWidth();
      final double wm = w-margin;
      for(final Node n:getChildren()){
        final double h = n.prefHeight(wm);
        layoutInArea(n, 0, x, w, h, 0, ii, false, false, HPos.CENTER, VPos.CENTER, true);
        x += h+s;
      }
    }
  }



  /**
   *
   * @return
   */
  public final BooleanProperty animationProperty(){
    if (animationProperty == null) {
      animationProperty = new SimpleBooleanProperty(this, "animation", true);
    }
    return animationProperty;
  }

  public final boolean isAnimation(){
    return animationProperty == null ? true : animationProperty.get();
  }

  public final void setAnimation(final boolean value){
    animationProperty().set(value);
  }

  private BooleanProperty animationProperty;


  public static enum ShowState{
    AUTO,SHOW,HIDE
  }



  /**
   *
   * @return
   */
  public final StyleableObjectProperty<ShowState> showStateProperty(){

    if (showStateProperty == null) {
      showStateProperty = new StyleableObjectProperty<ShowState>(ShowState.AUTO){
        @Override
        public String getName(){
          return "showState";
        }

        @Override
        public Object getBean(){
          return NToolBar.this;
        }

        @Override
        public CssMetaData<? extends Styleable, ShowState> getCssMetaData(){
          return SHOW;
        }
      };

      showStateProperty.addListener(o->{
        switch(getShowState()){
          case HIDE:
            showAnime.pause();
            showAnime.jumpTo(Duration.ZERO);
            sizeProperty.set(0);
            break;
          case SHOW:
            showAnime.pause();
            showAnime.jumpTo(getAnimationTime());
            sizeProperty.set(1);
            break;
          default:
            checkFocus(o);
            break;
        }
      });
    }
    return showStateProperty;
  }

  public final ShowState getShowState(){
    final ShowState s= showStateProperty == null ? ShowState.AUTO : showStateProperty.get();
    return s == null?ShowState.AUTO:s;
  }

  public final void setShowState(final ShowState value){
    showStateProperty().set(value);
  }

  private StyleableObjectProperty<ShowState> showStateProperty;
  @SuppressWarnings("unchecked")
  private static final CssMetaData<NToolBar, ShowState> SHOW= new CssMetaData<NToolBar, ShowState>("-fx-show",
      (StyleConverter<String,ShowState>)StyleConverter.getEnumConverter(ShowState.class),ShowState.AUTO){

    @Override
    public boolean isSettable(final NToolBar paramS){
      return paramS.orientationProperty==null || !paramS.orientationProperty.isBound();
    }

    @Override
    public StyleableProperty<ShowState> getStyleableProperty(final NToolBar paramS){
      return paramS.showStateProperty();
    }

  };


  /**
   *
   * @return
   */
  public final ObjectProperty<Duration> animationTimeProperty(){
    if (animationTimeProperty == null) {
      animationTimeProperty = new SimpleObjectProperty<>(this, "animationTime", DEFAULT_TIME);
      animationTimeProperty.addListener(o->{
        final Duration d = getAnimationTime();
        final KeyFrame keyFrame = showAnime.getKeyFrames().get(1);
        if(!keyFrame.getTime().equals(d)){
          new KeyFrame(d, new KeyValue(sizeProperty, 1));
          showAnime.getKeyFrames().set(1,keyFrame);
        }
      });
    }
    return animationTimeProperty;
  }

  public final Duration getAnimationTime(){
    final Duration d= animationTimeProperty == null ? DEFAULT_TIME : animationTimeProperty.get();
    return d ==null?DEFAULT_TIME:d;
  }

  public final void setAnimationTime(final Duration value){
    animationTimeProperty().set(value);
  }

  private ObjectProperty<Duration> animationTimeProperty;




  /**
   * 配置方向
   * @return
   */
  public final StyleableObjectProperty<Orientation> orientationProperty(){
    if (orientationProperty == null) {
      orientationProperty = new StyleableObjectProperty<Orientation>(Orientation.HORIZONTAL){

        @Override
        public CssMetaData<? extends Styleable, Orientation> getCssMetaData(){
          return ORIENTATION;
        }

        @Override
        public Object getBean(){
          return NToolBar.this;
        }

        @Override
        public String getName(){
          return "orientation";
        }};
    }
    return orientationProperty;
  }

  public final Orientation getOrientation(){
    final Orientation o= orientationProperty == null ? Orientation.HORIZONTAL : orientationProperty.get();
    return o == null?Orientation.HORIZONTAL:o;
  }
  public final boolean isHorizontal(){
    return getOrientation() == Orientation.HORIZONTAL;
  }

  public final boolean isVertical(){
    return getOrientation()==Orientation.VERTICAL;
  }
  public final void setOrientation(final Orientation value){
    orientationProperty().set(value);
  }

  private StyleableObjectProperty<Orientation> orientationProperty;

  @SuppressWarnings("unchecked")
  private static final CssMetaData<NToolBar, Orientation> ORIENTATION = new CssMetaData<NToolBar, Orientation>("-fx-orientation",
      (StyleConverter<String,Orientation>)StyleConverter.getEnumConverter(Orientation.class),Orientation.HORIZONTAL){

    @Override
    public boolean isSettable(final NToolBar paramS){
      return paramS.orientationProperty==null || !paramS.orientationProperty.isBound();
    }

    @Override
    public StyleableProperty<Orientation> getStyleableProperty(final NToolBar paramS){
      return paramS.orientationProperty();
    }

  };


  /**
   * itemの間隔
   * @return
   */
  public final StyleableDoubleProperty spacingProperty(){
    return spacingProperty;
  }

  public final double getSpacing(){
    return spacingProperty.get();
  }

  public final void setSpacing(final double value){
    spacingProperty.set(value);
  }

  private final StyleableDoubleProperty spacingProperty = new StyleableDoubleProperty(){

    @Override
    public String getName(){
      return "spacing";
    }

    @Override
    public Object getBean(){
      return NToolBar.this;
    }

    @Override
    public CssMetaData<? extends Styleable, Number> getCssMetaData(){
      return SPACING;
    }
  };//DoubleProperty(this, "spacing", 0);

  private static final CssMetaData<NToolBar, Number> SPACING = new CssMetaData<NToolBar, Number>("-fx-space",StyleConverter.getSizeConverter(),0){

    @Override
    public boolean isSettable(final NToolBar paramS){
      return !paramS.spacingProperty.isBound();
    }

    @Override
    public StyleableProperty<Number> getStyleableProperty(final NToolBar paramS){
      return paramS.spacingProperty;
    }

  };

  ///----------------------------------------------------------------------------

  private static final List <CssMetaData <? extends Styleable, ? > > cssMetaDataList;
  static {
      final List <CssMetaData <? extends Styleable, ? > > temp =
          new ArrayList <>(WindowRegion.getClassCssMetaData());
      temp.add(SPACING);
      temp.add(ORIENTATION);
      temp.add(SHOW);
      cssMetaDataList = Collections.unmodifiableList(temp);
  }


  public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() {
      return cssMetaDataList;
  }

   @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
      return getClassCssMetaData();
  }



}
