package nodamushi.jfx.markedj.application;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import javafx.stage.Stage;
import nodamushi.config.Settings;
import nodamushi.jfx.markedj.MarkedHTML;
import nodamushi.jfx.markedj.MarkedReader;
import nodamushi.jfx.markedj.MarkedView;
import nodamushi.jfx.markedj.MarkedHTML.CSS;
import nodamushi.jfx.markedj.MarkedHTML.Script;
import nodamushi.jfx.popup.TooltipBehavior;
import nodamushi.jfx.stage.NToolBar;
import nodamushi.jfx.util.KeyCombinations;
import nodamushi.nio.FileUtil;
import nodamushi.nio.FileWatcher;

public class MarkedjViewApp extends Application{
  private static final String APP_TITLE="markedj viewer";
  private ExtensionFilter MDFilter = new ExtensionFilter("mark down", "*.md","*.markdown");
  private ExtensionFilter HTMLFilter = new ExtensionFilter("html", "*.html");
  private ExtensionFilter AllFilter = new ExtensionFilter("*", "*.md");
  private static Settings SETTING;
  private static Path CSS_FILE;
  private static Path APP_DATA_DIR;
  private static List<Runnable> CSS_SETTING=new ArrayList<>();

  public static void main(final String[] args){
    SETTING = new Settings(MarkedjViewApp.class);
    if(SETTING.getBasePath().endsWith("bin")){
      SETTING.setBasePath(SETTING.getBasePath().getParent());
    }

    APP_DATA_DIR = SETTING.getBasePath().resolve("appdata").toAbsolutePath().normalize();
    CSS_FILE=APP_DATA_DIR.resolve("app.css");
    if(Files.isDirectory(APP_DATA_DIR)){
      FileWatcher.getDefault().watchFile(CSS_FILE, e->{
        Platform.runLater(()->{
          for(final Runnable r:CSS_SETTING){
            r.run();
          }
        });
      });
    }
    SETTING.readProperty("appdata/config");
    System.setProperty("prism.lcdtext", "false");
    launch(args);
  }

  private MarkedReader r;
  private MarkedHTML html;
  private MarkedView view;
  private Stage window;
  private File initDirectory = new File(System.getProperty("user.home"));
  private File saveInitDirectory = initDirectory;
  private String fileName;

  private static List<CSS> getWebCss(){
    final String css = SETTING.getProperty().get("markdown.cssfile").orElse("");
    final String[] arr = css.split(";");
    final List<CSS> list = new ArrayList<>();
    for(final String s:arr){
      final String c = s.trim();
      if(c.isEmpty()) {
        continue;
      }
      try{
        final Path p = Paths.get(c);
        list.add(new CSS(p));

      }catch(final Exception e){

      }
    }
    final String code = SETTING.getProperty().get("markdown.css").orElse("");
    if(!code.isEmpty()){
      list.add(new CSS(code));
    }
    return list;
  }

  private static List<Script> getMarkdownJs(){
    final String files = SETTING.getProperty().get("markdown.jsfile").orElse("");
    final String[] arr = files.split(";");
    final List<Script> list = new ArrayList<>();
    for(final String s:arr){
      final String[] ss = s.split(",",2);

      final String c = ss[0].trim();
      if(c.isEmpty()) {
        continue;
      }
      final String charset = ss.length==2?ss[1].trim():"utf-8";
      try{
        final Path p = Paths.get(c);
        list.add(new Script(p.toUri(), charset));
      }catch(final Exception e){

      }
    }
    final String script = SETTING.getProperty().get("markdown.js").orElse("");
    if(!script.isEmpty()){
      list.add(new Script(script));
    }
    return list;
  }

  @Override
  public void start(final Stage s) throws Exception{
    window = s;
    s.setTitle(APP_TITLE);
    final String langPrefix=SETTING.getProperty().get("markdown.code.langprefix").orElse("");
    MarkedReader.DEFAULT_OPTIONS.setLangPrefix(langPrefix);


    final Button open = new Button("Open");
    final Button reload = new Button("Reload");
    final Button save = new Button("Save");
    final Button setting = new Button("Setting");


    open.setId("open-menu");
    reload.setId("reload-menu");
    save.setId("save-menu");
    setting.setId("setting-menu");

    open.setOnAction(this::openFile);
    reload.setOnAction(this::reload);
    save.setOnAction(this::saveFile);
    setting.setOnAction(this::setting);

    final TooltipBehavior behavior = new TooltipBehavior();
    behavior.setPopupUpdater((t,n)->{
      t.setText(((Button)n).getText());
    });
    behavior.setOpenDuration(Duration.millis(100));
    behavior.install(open);
    behavior.install(reload);
    behavior.install(save);
    behavior.install(setting);


    final NToolBar mbar = new NToolBar(5,open,reload,save,setting);



    view = new MarkedView();
    view.setMarkedHtml(html=new MarkedHTML());
    final List<String> unnamed = getParameters().getUnnamed();
    if(!unnamed.isEmpty()){
      final String path = unnamed.get(0);
      try{
        final Path p = Paths.get(path);
        open(p);
      }catch(final Exception e){}
    }

    final BorderPane p = new BorderPane();
    p.setCenter(view);
    p.setTop(mbar);

    final Scene scene = new Scene(p);

    final KeyCombinations keymap = new KeyCombinations();
    keymap.add(sc-> reload(null), KeyCode.R,KeyCodeCombination.SHORTCUT_DOWN);
    keymap.add(sc-> reload(null), KeyCode.HOME);
    keymap.add(sc-> reload(null), KeyCode.F5);
    keymap.add(sc-> openFile(null), KeyCode.O,KeyCodeCombination.SHORTCUT_DOWN);
    keymap.add(sc-> saveFile(null), KeyCode.S,KeyCodeCombination.SHORTCUT_DOWN);
    keymap.add(sc-> setting(null), KeyCode.E,KeyCodeCombination.SHORTCUT_DOWN);
    keymap.install(scene);
    if(Files.isReadable(CSS_FILE)){
      scene.getStylesheets().setAll(CSS_FILE.toUri().toString());
    }

    CSS_SETTING.add(()->{
      if(Files.isReadable(CSS_FILE)){
        scene.getStylesheets().setAll(CSS_FILE.toUri().toString());
      }else{
        scene.getStylesheets().clear();
      }
    });

    scene.addEventFilter(DragEvent.DRAG_OVER, event->{
      final Dragboard db = event.getDragboard();
      if (db.hasFiles()) {
        event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
      }
    });


    // Dropping over surface
    scene.addEventFilter(DragEvent.DRAG_DROPPED, event->{
      final Dragboard db = event.getDragboard();
      boolean success = false;
      if (db.hasFiles()) {
        success = true;
        final List<File> f=db.getFiles();
        if(!f.isEmpty()){
          final File file = f.get(0);
          final Path path =file.toPath();
          open(path);
          initDirectory = file.getParentFile();
          saveInitDirectory=file.getParentFile();
        }
        event.setDropCompleted(success);
        event.consume();
        window.requestFocus();
      }
    });

    s.setScene(scene);
    s.show();


    html.getLinks().addAll(getWebCss());
    html.getScripts().addAll(getMarkdownJs());
    final String pre=SETTING.getProperty().get("markdown.body.pre").orElse("");

    if(!pre.isEmpty()){
      html.setPreBody(pre);
    }
    final String post=SETTING.getProperty().get("markdown.body.post").orElse("");
    if(!post.isEmpty()){
      html.setPostBody(post);
    }
  }




  private void reload(final ActionEvent e){
    if(r!=null){
      r.parse();
    }
    view.reload();
  }
  private void openFile(final ActionEvent e){
    final FileChooser f = new FileChooser();
    f.setInitialDirectory(initDirectory);
    if(fileName!=null){
      f.setInitialFileName(fileName+".md");
    }
    f.getExtensionFilters().addAll(MDFilter,AllFilter);
    final File file = f.showOpenDialog(window);
    if(file!=null){
      saveInitDirectory=initDirectory = file.getParentFile();
      open(file.toPath());
    }
  }

  private void open(final Path p){
    try{
      if(r!=null){
        r.unwatchFile();
      }
      r = new MarkedReader(p);
      r.watchFile();
      html.setReader(r);
      fileName = p.getFileName().toString();
      final int index =fileName.lastIndexOf('.');
      if(index != -1){
        fileName = fileName.substring(0, index);
      }
      window.setTitle(APP_TITLE+":"+p.toString());
    }catch(final Exception e){

    }
  }

  private void saveFile(final ActionEvent e){
    if(r==null){
      return;
    }
    final FileChooser f = new FileChooser();

    f.setInitialDirectory(saveInitDirectory);
    if(fileName!=null){
      f.setInitialFileName(fileName+".html");
    }
    f.getExtensionFilters().addAll(HTMLFilter,AllFilter);
    final File file = f.showSaveDialog(window);
    if(file!=null){
      saveInitDirectory = file.getParentFile();

      save(file.toPath());
    }
  }

  private void save(final Path p){
    if(r==null){
      return;
    }
    try{
      final String s = html.getHtml();
      FileUtil.writeText(s, p);
    }catch(final Exception e){

    }
  }

  private void setting(final ActionEvent e){
    //TODO
    final Alert a = new Alert(AlertType.INFORMATION);
    a.setTitle("未実装");
    a.setHeaderText("Configいじって");
    final TextField f = new TextField(SETTING.getBasePath().resolve("appdata/config").toString());

    a.getDialogPane().setContent(f);
    a.initOwner(window);
    a.show();
  }

}
