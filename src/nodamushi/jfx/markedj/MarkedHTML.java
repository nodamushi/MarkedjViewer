package nodamushi.jfx.markedj;

import static java.util.Objects.*;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MarkedHTML{

  public static class Link{
    private final URI href;
    private final String rel;
    private final String type;
    public Link(final Path href,final String rel,final String type){
      this.href = href.normalize().toAbsolutePath().toUri();
      this.rel = requireNonNull(rel,"rel");
      this.type =type;
    }
    public Link(final URI href,final String rel,final String type){
      this.href = requireNonNull(href,"href");
      this.rel = requireNonNull(rel,"rel");
      this.type =type;
    }
    protected Link(){
      href = null;
      rel = null;
      type = null;
    }
    public URI getHref(){
      return href;
    }
    public String getRel(){
      return rel;
    }
    public String getType(){
      return type;
    }
    @Override
    public String toString(){
      if(type==null || type.isEmpty()){
        return "<link rel='"+rel+"' href='"+href.toString()+"'>";
      }
      return "<link rel='"+rel+"' href='"+href.toString()+"' type='"+type+"'>";
    }
  }

  public static class CSS extends Link{
    boolean stylemode;
    private String code;
    public CSS(final Path href){
      super(href,"stylesheet","text/css");
      stylemode = false;
    }

    public CSS(final String code){
      stylemode=true;
      this.code = code;
    }
    @Override
    public String toString(){
      if(stylemode){
        return "<style type=\"text/css\">\n"+code+"\n</style>";
      }else{
        return super.toString();
      }
    }
  }


  public static class Script{
    private final URI src;
    private final String charset;
    private final String script;
    public Script(final URI src,final String charset){
      this.src =requireNonNull(src);
      this.charset = charset;
      script = null;
    }
    public Script(final String script){
      src = null;
      charset = null;
      this.script = requireNonNull(script);
    }

    @Override
    public String toString(){
      if(script==null){
        if(charset==null || charset.isEmpty()) {
          return "<script src='"+src.toString()+"'></script>";
        } else {
          return "<script src='"+src.toString()+"' charset='"+charset+"'></script>";
        }
      }else{
        return "<script>"+script+"</script>";
      }
    }
  }



  public MarkedHTML(){
    links.addListener(this::invalidateHTML);
    scripts.addListener(this::invalidateHTML);
    htmlWrapper.bind(htmlContents);
  }

  private ObservableList<Link> links = FXCollections.observableArrayList();
  private ObservableList<Script> scripts = FXCollections.observableArrayList();

  public ObservableList<Link> getLinks(){
    return links;
  }
  public ObservableList<Script> getScripts(){
    return scripts;
  }

  /**
   *
   * @return
   */
  public final ReadOnlyStringProperty htmlProperty(){
    return htmlWrapper.getReadOnlyProperty();
  }
  public final String getHtml(){return htmlWrapper.get();}
  private final ReadOnlyStringWrapper htmlWrapper = new ReadOnlyStringWrapper(this, "html", null);
  private final StringBinding htmlContents = new StringBinding(){
    @Override
    protected String computeValue(){
      final StringBuilder sb = new StringBuilder();
      final MarkedReader r = getReader();
      sb.append("<!DOCTYPE html>\n<html>\n<head>\n");
      if(r!=null){
        final Path directory = r.getDirectory().toAbsolutePath().normalize();
        final URI uri = directory.toUri();
        sb.append("<base href=\"").append(uri).append("\">\n");
      }
      for(final Link l:links){
        sb.append(l.toString()).append('\n');
      }
      for(final Script l:scripts){
        sb.append(l.toString()).append('\n');
      }

      sb.append("</head>\n<body>\n")
      .append(getPreBody());
      if(r!=null && r.getCode()!=null){
        sb.append("\n\n<!-- start markdown -->\n\n");
        sb.append(r.getCode());
        sb.append("\n\n<!-- end markdown -->\n\n");
      }
      sb.append(getPostBody()).append("\n</body>\n</html>");

      return sb.toString();
    }
  };
  private void invalidateHTML(final Observable o){htmlContents.invalidate();}

  /**
   *
   * @return
   */
  public final ObjectProperty<MarkedReader> readerProperty(){
    if (readerProperty == null) {
      readerProperty = new SimpleObjectProperty<>(this, "reader", null);
      readerProperty.addListener((ov,o,n)->{
        if(o!=null){
          o.codeProperty().removeListener(this::invalidateHTML);
        }
        if(n!=null){
          n.codeProperty().addListener(this::invalidateHTML);
        }
        changeReader = true;
        invalidateHTML(ov);
      });
    }
    return readerProperty;
  }



  public final MarkedReader getReader(){
    return readerProperty == null ? null : readerProperty.get();
  }

  public final void setReader(final MarkedReader value){
    readerProperty().set(value);
  }

  private ObjectProperty<MarkedReader> readerProperty;



  private boolean changeReader=true;

  public boolean isChangeURI(){
    final boolean b = changeReader;
    changeReader = false;
    return b;
  }



  /**
   *
   * @return
   */
  public final StringProperty headerProperty(){
    if (headerProperty == null) {
      headerProperty = new SimpleStringProperty(this, "header", "");
      headerProperty.addListener(this::invalidateHTML);
    }
    return headerProperty;
  }

  public final String getHeader(){
    return headerProperty == null ? "" : headerProperty.get();
  }

  public final void setHeader(final String value){
    headerProperty().set(value);
  }

  private StringProperty headerProperty;


  /**
   *
   * @return
   */
  public final StringProperty preBodyProperty(){
    if (preBodyProperty == null) {
      preBodyProperty = new SimpleStringProperty(this, "preBody", "");
      preBodyProperty.addListener(this::invalidateHTML);
    }
    return preBodyProperty;
  }

  public final String getPreBody(){
    return preBodyProperty == null ? "" : preBodyProperty.get();
  }

  public final void setPreBody(final String value){
    preBodyProperty().set(value);
  }

  private StringProperty preBodyProperty;


  /**
   *
   * @return
   */
  public final StringProperty postBodyProperty(){
    if (postBodyProperty == null) {
      postBodyProperty = new SimpleStringProperty(this, "postBody", "");
      postBodyProperty.addListener(this::invalidateHTML);
    }
    return postBodyProperty;
  }

  public final String getPostBody(){
    return postBodyProperty == null ? "" : postBodyProperty.get();
  }

  public final void setPostBody(final String value){
    postBodyProperty().set(value);
  }

  private StringProperty postBodyProperty;


}
