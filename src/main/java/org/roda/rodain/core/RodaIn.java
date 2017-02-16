package org.roda.rodain.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.roda.rodain.creation.ui.CreationModalPreparation;
import org.roda.rodain.creation.ui.CreationModalStage;
import org.roda.rodain.inspection.InspectionPane;
import org.roda.rodain.rules.VisitorStack;
import org.roda.rodain.rules.filters.IgnoredFilter;
import org.roda.rodain.schema.ClassificationSchema;
import org.roda.rodain.schema.DescriptionObject;
import org.roda.rodain.schema.ui.SchemaNode;
import org.roda.rodain.schema.ui.SchemaPane;
import org.roda.rodain.source.ui.FileExplorerPane;
import org.roda.rodain.source.ui.items.SourceTreeItem;
import org.roda.rodain.utils.FontAwesomeImageCreator;
import org.roda.rodain.utils.ModalStage;
import org.roda.rodain.utils.OpenPathInExplorer;
import org.roda.rodain.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 16-09-2015.
 */
public class RodaIn extends Application {
  private static final Logger LOGGER = LoggerFactory.getLogger(RodaIn.class.getName());
  private static final String LATEST_VERSION_LINK = "https://github.com/keeps/roda-in/releases";
  private static Stage stage;

  private BorderPane mainPane;
  private double initialWidth = 1200, initialHeight = 700;

  // Splash
  private Pane splashPane;
  private Stage splashStage;

  private static FileExplorerPane fileExplorer;
  private static InspectionPane inspectionPane;
  private static SchemaPane schemePane;

  // Languages
  private RadioMenuItem langEN, langPT, langHU, langES_CL;

  private static long lastMessage = System.currentTimeMillis();

  /**
   * The entry point of the application.
   *
   * @param args
   *          The arguments passed to the application.
   */
  public static void startApp(String[] args) {
    launch(args);
  }

  @Override
  public void init() {
    ImageView splash = new ImageView();
    try {
      splash = new ImageView(new Image(ClassLoader.getSystemResource("roda-in-splash.png").openStream()));
    } catch (IOException e) {
      LOGGER.error("Error reading logo file", e);
    }

    splashPane = new Pane();
    splashPane.setStyle("-fx-background-color: transparent");
    splashPane.getChildren().add(splash);
    splashPane.setCache(true);
    splashPane.setCacheHint(CacheHint.SPEED);
    splashPane.setEffect(new DropShadow());
  }

  /**
   * Creates the interface structure and loads resources.
   * <p/>
   * <p>
   * This method sets the application logo, loads fonts, styles and property
   * files. Furthermore, creates the frame structure and the menu. The frame
   * structure is a SplitPane, split in three sections - file explorer,
   * classification scheme, inspection - and a footer.
   * </p>
   *
   * @param primaryStage
   */
  @Override
  public void start(Stage primaryStage) {
    Thread.setDefaultUncaughtExceptionHandler(RodaIn::showError);
    if (splashPane != null) {
      splashStage = new Stage();
      Scene splashScene = new Scene(splashPane);
      splashScene.setFill(Color.TRANSPARENT);
      splashStage.setScene(splashScene);
      splashStage.initOwner(stage);
      splashStage.initStyle(StageStyle.TRANSPARENT);
      splashStage.show();
      splashStage.centerOnScreen();
    }

    stage = primaryStage;
    stage.setMinWidth(1024);
    stage.setMinHeight(600);

    try {
      stage.getIcons().add(new Image(ClassLoader.getSystemResource("roda2-logo.png").openStream()));
    } catch (IOException e) {
      LOGGER.error("Error reading logo file", e);
    }

    Task<Void> initTask = new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        stage.setOnCloseRequest(event -> closeApp());

        AppProperties.initialize();
        String ignoredRaw = AppProperties.getAppConfig("app.ignoredFiles");
        if(ignoredRaw!=null && !ignoredRaw.trim().equalsIgnoreCase("")){
          String[] ignored = ignoredRaw.split(",");
          for (String s : ignored) {
            IgnoredFilter.addIgnoreRule(s);
          }
        }
        
        // load the custom fonts
        Font.loadFont(ClassLoader.getSystemResource("fonts/Ubuntu-Regular.ttf").toExternalForm(), 10);
        Font.loadFont(ClassLoader.getSystemResource("fonts/Ubuntu-Medium.ttf").toExternalForm(), 10);
        Font.loadFont(ClassLoader.getSystemResource("fonts/Ubuntu-Light.ttf").toExternalForm(), 10);

        createFrameStructure();
        createMenu();

        // setup and show the window
        stage.setTitle("RODA-In");
        return null;
      }
    };

    initTask.setOnSucceeded(event -> {
      Scene scene = new Scene(mainPane, initialWidth, initialHeight);

      scene.getStylesheets().add(ClassLoader.getSystemResource("css/mainWindow.css").toExternalForm());
      scene.getStylesheets().add(ClassLoader.getSystemResource("css/shared.css").toExternalForm());
      scene.getStylesheets().add(ClassLoader.getSystemResource("css/xml-highlighting.css").toExternalForm());
      stage.setScene(scene);

      stage.show();
      stage.centerOnScreen();
      stage.setMaximized(true);
      if (splashStage != null)
        splashStage.close();

      // Add the bindings after stage.show(), otherwise they'll start as 0
      Footer.addBindings(fileExplorer);

      String rodaInEnv = System.getenv("RODAIN_ENV");
      if (rodaInEnv == null || !rodaInEnv.equals("testing")) {
        checkForUpdates();
      }
    });

    initTask.exceptionProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        LOGGER.error("Error initializing application", newValue);
      }
    });

    initTask.setOnFailed(event -> {
      LOGGER.error("Failed application initialization");
      if (splashStage != null)
        splashStage.close();
    });

    new Thread(initTask).start();
  }

  private static void showError(Thread t, Throwable e) {
    if (Platform.isFxApplicationThread()) {
      LOGGER.error("Unexpected error", e);
      showErrorDialog(e);
    } else {
      System.err.println("An unexpected error occurred in " + t);
    }
  }

  private static void showErrorDialog(Throwable e) {
    if (System.currentTimeMillis() - lastMessage > 500) {
      lastMessage = System.currentTimeMillis();
      Stage dialog = new Stage();
      dialog.initModality(Modality.APPLICATION_MODAL);
      Parent root = new HBox();
      dialog.setScene(new Scene(root, 400, 550));
      dialog.show();
      dialog.centerOnScreen();
      dialog.close();

      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.initStyle(StageStyle.DECORATED);
      alert.initOwner(dialog);
      alert.setTitle(I18n.t("genericError.title"));
      alert.setHeaderText(I18n.t("genericError.title"));
      StringBuilder content = new StringBuilder(I18n.t("genericError.content"));
      content.append("\n\n");
      content.append(e.toString());
      alert.setContentText(content.toString());
      alert.getDialogPane().setStyle(AppProperties.getStyle("export.alert"));

      // Create expandable Exception.
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println(e.getMessage());
      for (StackTraceElement ste : e.getStackTrace()) {
        pw.println("\t" + ste);
      }
      String exceptionText = sw.toString();

      Label label = new Label(I18n.t("CreationModalProcessing.alert.stacktrace"));

      TextArea textArea = new TextArea(exceptionText);
      textArea.setWrapText(true);
      textArea.setEditable(false);
      textArea.minWidthProperty().bind(alert.getDialogPane().widthProperty().subtract(20));
      textArea.maxWidthProperty().bind(alert.getDialogPane().widthProperty().subtract(20));

      GridPane expContent = new GridPane();
      expContent.setMaxWidth(Double.MAX_VALUE);
      expContent.add(label, 0, 0);
      expContent.add(textArea, 0, 1);

      textArea.minHeightProperty().bind(expContent.heightProperty().subtract(50));
      // Set expandable Exception into the dialog pane.
      alert.getDialogPane().setExpandableContent(expContent);
      alert.getDialogPane().minHeightProperty().bindBidirectional(dialog.minHeightProperty());
      alert.getDialogPane().setMinWidth(500);
      alert.getDialogPane().setMinHeight(275);

      // Without this setStyle the pane won't resize correctly. Black magic...
      alert.getDialogPane().setStyle(AppProperties.getStyle("creationmodalprocessing.blackmagic"));

      alert.show();
    }
  }

  private void createFrameStructure() {
    mainPane = new BorderPane();
    mainPane.getStyleClass().add("border-pane");
    mainPane.setCenter(createSplitPane());

    // Create Footer after the center because Footer needs to bind to some
    // properties of panes in the center
    Platform.runLater(() -> {
      Footer footer = Footer.getInstance();
      mainPane.setBottom(footer);
    });
  }

  private SplitPane createSplitPane() {
    // Divide center pane in 3
    SplitPane split = new SplitPane();
    // schemePane must be created before fileExplorer because of some variable
    // bindings
    schemePane = new SchemaPane(stage);
    fileExplorer = new FileExplorerPane(stage);
    inspectionPane = new InspectionPane(stage);

    split.setDividerPositions(0.33, 0.67);
    split.getItems().addAll(fileExplorer, schemePane, inspectionPane);

    return split;
  }

  private void createMenu() {
    MenuBar menu = new MenuBar();
    Menu menuFile = new Menu(I18n.t("Main.file"));

    Menu menuEdit = new Menu(I18n.t("Main.edit"));
    Menu menuClassScheme = new Menu(I18n.t("Main.classScheme"));
    Menu menuView = new Menu(I18n.t("Main.view"));
    Menu menuHelp = new Menu(I18n.t("Main.help"));

    Menu language = new Menu(I18n.t("Main.language"));

    Platform.runLater(
      () -> language.setGraphic(new ImageView(FontAwesomeImageCreator.generate(FontAwesomeImageCreator.GLOBE))));

    // File
    final ToggleGroup languageGroup = new ToggleGroup();
    langPT = new RadioMenuItem("Português");
    langPT.setUserData("pt");
    langPT.setToggleGroup(languageGroup);
    langEN = new RadioMenuItem("English");
    langEN.setUserData("en");
    langEN.setToggleGroup(languageGroup);
    langHU = new RadioMenuItem("Magyar");
    langHU.setUserData("hu");
    langHU.setToggleGroup(languageGroup);
    langES_CL = new RadioMenuItem("Español (Chile)");
    langES_CL.setUserData("es_CL");
    langES_CL.setToggleGroup(languageGroup);
    language.getItems().addAll(langEN, langPT, langHU, langES_CL);

    updateSelectedLanguageMenu();

    languageGroup.selectedToggleProperty().addListener(observable -> {
      if (languageGroup.getSelectedToggle() != null) {
        String lang = (String) languageGroup.getSelectedToggle().getUserData();
        if (!lang.equals(AppProperties.getLocale().getLanguage())) {
          Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
          dlg.getButtonTypes().clear();
          dlg.getButtonTypes().addAll(new ButtonType(I18n.t("cancel", lang), ButtonBar.ButtonData.CANCEL_CLOSE),
            new ButtonType(I18n.t("restart", lang), ButtonBar.ButtonData.OK_DONE));
          dlg.initStyle(StageStyle.UNDECORATED);
          dlg.setHeaderText(I18n.t("Main.updateLang.header", lang));
          dlg.setTitle(I18n.t("Main.updateLang.title", lang));
          dlg.setContentText(I18n.t("Main.updateLang.content", lang));
          dlg.initModality(Modality.APPLICATION_MODAL);
          dlg.initOwner(stage);
          dlg.show();
          dlg.resultProperty().addListener(o -> confirmLanguageChange(lang, dlg.getResult()));
        }
      }
    });

    final MenuItem openConfigurationFolder = new MenuItem(I18n.t("Main.openConfigurationFolder"));
    openConfigurationFolder.setAccelerator(KeyCombination.keyCombination("Ctrl+G"));
    openConfigurationFolder.setOnAction(event -> OpenPathInExplorer.open(AppProperties.getRodainPath()));

    final MenuItem openFolder = new MenuItem(I18n.t("Main.addFolder"));
    openFolder.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
    openFolder.setOnAction(event -> fileExplorer.chooseNewRoot());

    final MenuItem createSIPs = new MenuItem(I18n.t("Main.exportSips"));
    createSIPs.setAccelerator(KeyCombination.keyCombination("Ctrl+X"));
    createSIPs.setOnAction(event -> exportSIPs());

    final MenuItem quit = new MenuItem(I18n.t("Main.quit"));
    quit.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));
    quit.setOnAction(event -> closeApp());
    final MenuItem reset = new MenuItem(I18n.t("Main.reset"));
    reset.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
    reset.setOnAction(event -> {
      Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
      dlg.initStyle(StageStyle.UNDECORATED);
      dlg.setHeaderText(I18n.t("Main.confirmReset.header"));
      dlg.setTitle(I18n.t("Main.reset"));
      dlg.setContentText(I18n.t("Main.confirmReset.content"));
      dlg.initModality(Modality.APPLICATION_MODAL);
      dlg.initOwner(stage);
      dlg.showAndWait();

      if (dlg.getResult().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
        PathCollection.reset();
        inspectionPane = new InspectionPane(stage);
        fileExplorer = new FileExplorerPane(stage);
        schemePane = new SchemaPane(stage);
        mainPane.setCenter(createSplitPane());
        schemePane.showHelp();
      }
    });

    menuFile.getItems().addAll(reset, openFolder, createSIPs, openConfigurationFolder, quit);

    // Classification scheme
    final MenuItem createCS = new MenuItem(I18n.t("Main.createCS"));
    createCS.setAccelerator(KeyCombination.keyCombination("Ctrl+R"));
    createCS.setOnAction(event -> schemePane.createClassificationScheme());

    final MenuItem updateCS = new MenuItem(I18n.t("Main.loadCS"));
    updateCS.setAccelerator(KeyCombination.keyCombination("Ctrl+L"));
    updateCS.setOnAction(event -> schemePane.loadClassificationSchema());

    final MenuItem exportCS = new MenuItem(I18n.t("Main.exportCS"));
    exportCS.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
    exportCS.setOnAction(event -> {
      FileChooser chooser = new FileChooser();
      chooser.setTitle(I18n.t("filechooser.title"));
      File selectedFile = chooser.showSaveDialog(stage);
      if (selectedFile == null)
        return;
      exportCS(selectedFile.toPath().toString());
    });

    menuClassScheme.getItems().addAll(createCS, updateCS, exportCS);

    // Edit
    final MenuItem ignoreItems = new MenuItem(I18n.t("Main.ignoreItems"));
    ignoreItems.setAccelerator(KeyCombination.keyCombination("DELETE"));
    ignoreItems.setOnAction(event -> fileExplorer.ignore());

    menuEdit.getItems().addAll(ignoreItems);

    // View
    final MenuItem showFiles = new MenuItem(I18n.t("Main.hideFiles"));
    showFiles.setAccelerator(KeyCombination.keyCombination("Ctrl+F"));
    showFiles.setOnAction(event -> {
      fileExplorer.toggleFilesShowing();
      if (FileExplorerPane.isShowFiles())
        showFiles.setText(I18n.t("Main.hideFiles"));
      else
        showFiles.setText(I18n.t("Main.showFiles"));
    });
    final MenuItem showIgnored = new MenuItem(I18n.t("Main.showIgnored"));
    showIgnored.setAccelerator(KeyCombination.keyCombination("Ctrl+I"));
    showIgnored.setOnAction(event -> {
      fileExplorer.toggleIgnoredShowing();
      if (FileExplorerPane.isShowIgnored())
        showIgnored.setText(I18n.t("Main.hideIgnored"));
      else
        showIgnored.setText(I18n.t("Main.showIgnored"));
    });
    final MenuItem showMapped = new MenuItem(I18n.t("Main.showMapped"));
    showMapped.setAccelerator(KeyCombination.keyCombination("Ctrl+M"));
    showMapped.setOnAction(event -> {
      fileExplorer.toggleMappedShowing();
      if (FileExplorerPane.isShowMapped())
        showMapped.setText(I18n.t("Main.hideMapped"));
      else
        showMapped.setText(I18n.t("Main.showMapped"));
    });

    menuView.getItems().addAll(showFiles, showIgnored, showMapped);

    // Help
    final MenuItem checkVersion = new MenuItem(I18n.t("Main.checkVersion"));
    checkVersion.setAccelerator(KeyCombination.keyCombination("Ctrl+U"));
    checkVersion.setOnAction(event -> {
      if (!checkForUpdates()) {
        try {
          Alert dlg = new Alert(Alert.AlertType.INFORMATION);
          dlg.initStyle(StageStyle.UNDECORATED);
          dlg.setHeaderText(String.format(I18n.t("Main.noUpdates.header"), Utils.getCurrentVersion()));
          dlg.setContentText(I18n.t("Main.noUpdates.content"));
          dlg.initModality(Modality.APPLICATION_MODAL);
          dlg.initOwner(stage);

          dlg.getDialogPane().setMinWidth(300);
          dlg.show();
        } catch (ConfigurationException e) {
          LOGGER.debug("Unable to get version.", e);
        }
      }
    });

    String startingValue = AppProperties.getAppConfig("app.helpEnabled");
    String showHelpText;
    if (Boolean.parseBoolean(startingValue)) {
      showHelpText = I18n.t("Main.hideHelp");
    } else {
      showHelpText = I18n.t("Main.showHelp");
    }
    final MenuItem showHelp = new MenuItem(showHelpText);
    showHelp.setAccelerator(KeyCombination.keyCombination("Ctrl+H"));
    showHelp.setOnAction(event -> {
      String currentValue = AppProperties.getAppConfig("app.helpEnabled");
      if (Boolean.parseBoolean(currentValue)) {
        showHelp.setText(I18n.t("Main.showHelp"));
        AppProperties.setAppConfig("app.helpEnabled", "false");
        currentValue = I18n.t("Main.hideHelp");
        AppProperties.saveAppConfig();
      } else {
        showHelp.setText(I18n.t("Main.hideHelp"));
        AppProperties.setAppConfig("app.helpEnabled", "true");
        currentValue = I18n.t("Main.showHelp");
        AppProperties.saveAppConfig();
      }
      Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
      dlg.getButtonTypes().clear();
      dlg.getButtonTypes().addAll(new ButtonType(I18n.t("cancel"), ButtonBar.ButtonData.CANCEL_CLOSE),
        new ButtonType(I18n.t("restart"), ButtonBar.ButtonData.OK_DONE));
      dlg.initStyle(StageStyle.UNDECORATED);
      dlg.setHeaderText(currentValue);
      dlg.setContentText(I18n.t("Main.updateLang.content"));
      dlg.initModality(Modality.APPLICATION_MODAL);
      dlg.initOwner(stage);
      dlg.show();
      dlg.resultProperty().addListener(o -> confirmRestart(dlg.getResult()));
    });

    final MenuItem helpPage = new MenuItem(I18n.t("Main.helpPage"));
    helpPage.setOnAction(event -> {
      ModalStage modalStage = new ModalStage(stage);
      modalStage.setRoot(new HelpModal(modalStage), false);
    });

    menuHelp.getItems().addAll(language, checkVersion, showHelp, helpPage);

    menu.getMenus().addAll(menuFile, menuEdit, menuClassScheme, menuView, menuHelp);
    mainPane.setTop(menu);
  }

  private void updateSelectedLanguageMenu() {
    switch (AppProperties.getLocale().toLanguageTag().toLowerCase()) {
      case "en":
        langEN.setSelected(true);
        break;
      case "pt-pt":
      case "pt-br":
      case "pt":
        langPT.setSelected(true);
        break;
      case "hu":
        langHU.setSelected(true);
        break;
      case "es":
      case "es-cl":
        langES_CL.setSelected(true);
        break;
      default:
        langEN.setSelected(true);
        break;
    }
  }

  private void confirmRestart(ButtonType result) {
    if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
      restartApplication();
    }
  }

  private void confirmLanguageChange(String lang, ButtonType result) {
    if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
      AppProperties.setAppConfig("app.language", lang);
      AppProperties.saveAppConfig();
      restartApplication();
    } else {
      updateSelectedLanguageMenu();
    }
  }

  private void restartApplication() {
    try {
      final File currentExecutable = new File(RodaIn.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      /* is it a jar or exe file? */
      if (currentExecutable.getName().endsWith(".jar")) {
        /* Build command: java -jar application.jar */
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(currentExecutable.getPath());

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.start();
        Platform.exit();
      } else if (currentExecutable.getName().endsWith(".exe")) {
        OpenPathInExplorer.open(currentExecutable.toPath());
        Platform.exit();
      }
    } catch (URISyntaxException e) {
      LOGGER.error("Error creating URI when restarting the application", e);
    } catch (IOException e) {
      LOGGER.error("Error creating the process to restart the application", e);
    }
  }

  private static void closeApp() {
    exportCS(AppProperties.getRodainPath().resolve(".plan.temp").toString());
    VisitorStack.end();
    Footer.getInstance().cancelMemoryAutoUpdater();
    Platform.exit();
  }

  private static boolean checkForUpdates() {
    try {
      Date currentVersion = Utils.getCurrentVersionBuildDate();
      Date latestVersion = Utils.getLatestVersionBuildDate();

      if (currentVersion != null && latestVersion != null) {
        if (currentVersion.compareTo(latestVersion) < 0) {
          String content = String.format(I18n.t("Main.newVersion.content"), Utils.getCurrentVersion(),
            Utils.getLatestVersion());
          Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
          dlg.initStyle(StageStyle.UNDECORATED);
          dlg.setHeaderText(I18n.t("Main.newVersion.header"));
          dlg.setTitle("");
          dlg.setContentText(content);
          dlg.initModality(Modality.APPLICATION_MODAL);
          dlg.initOwner(stage);

          dlg.getDialogPane().setMinWidth(300);
          dlg.showAndWait();

          if (dlg.getResult().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            OpenPathInExplorer.open(LATEST_VERSION_LINK);
          }
          return true;
        }
      }
    } catch (ConfigurationException e) {
      LOGGER.error("Could not retrieve application version from build.properties", e);
    } catch (URISyntaxException e) {
      LOGGER.warn("The URI is malformed", e);
    } catch (IOException e) {
      LOGGER.warn("Error accessing the GitHub API", e);
    }
    return false;
  }

  private static void exportCS(String outputFile) {
    Set<SchemaNode> nodes = schemePane.getSchemaNodes();
    List<DescriptionObject> dobjs = new ArrayList<>();
    for (SchemaNode sn : nodes) {
      dobjs.add(sn.getDob());
    }
    ClassificationSchema cs = new ClassificationSchema();
    cs.setDos(dobjs);
    cs.export(outputFile);
    AppProperties.setAppConfig("lastClassificationScheme", outputFile);
    AppProperties.saveAppConfig();
  }

  /**
   * @return The selected items in the file explorer
   */
  public static Set<SourceTreeItem> getSourceSelectedItems() {
    return fileExplorer.getSelectedItems();
  }

  /**
   * @return The inspection pane object.
   */
  public static InspectionPane getInspectionPane() {
    return inspectionPane;
  }

  /**
   * @return The file explorer object.
   */
  public static FileExplorerPane getFileExplorer() {
    return fileExplorer;
  }

  /**
   * @return The scheme pane object.
   */
  public static SchemaPane getSchemePane() {
    return schemePane;
  }

  /**
   * @return The Map with the selected SIPs of all the SchemaNodes in the scheme
   *         pane
   */
  public static Map<DescriptionObject, List<String>> getSelectedDescriptionObjects() {
    return schemePane.getSelectedDescriptionObjects();
  }

  /**
   * @return The Map with all the SIPs of all the SchemaNodes in the scheme pane
   */
  public static Map<DescriptionObject, List<String>> getAllDescriptionObjects() {
    return schemePane.getAllDescriptionObjects();
  }

  /**
   * Shows a pane to start the export process of the created SIPs.
   */
  public static void exportSIPs() {
    // force the edits to the metadata text area to be saved
    inspectionPane.saveMetadata();

    CreationModalStage creationStage = new CreationModalStage(stage);
    CreationModalPreparation pane = new CreationModalPreparation(creationStage);
    creationStage.setRoot(pane);
  }
}
