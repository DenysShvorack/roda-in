package org.roda.rodain.ui.schema.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.roda.rodain.core.ConfigurationManager;
import org.roda.rodain.core.Constants;
import org.roda.rodain.core.Constants.PathState;
import org.roda.rodain.core.Controller;
import org.roda.rodain.core.I18n;
import org.roda.rodain.core.schema.ClassificationSchema;
import org.roda.rodain.core.schema.DescriptiveMetadata;
import org.roda.rodain.core.schema.Sip;
import org.roda.rodain.core.sip.SipPreview;
import org.roda.rodain.ui.Footer;
import org.roda.rodain.ui.ModalStage;
import org.roda.rodain.ui.RodaInApplication;
import org.roda.rodain.ui.inspection.AddMetadataPane;
import org.roda.rodain.ui.rules.Rule;
import org.roda.rodain.ui.rules.ui.RuleModalController;
import org.roda.rodain.ui.source.SourceTreeCell;
import org.roda.rodain.ui.source.items.SourceTreeDirectory;
import org.roda.rodain.ui.source.items.SourceTreeFile;
import org.roda.rodain.ui.source.items.SourceTreeItem;
import org.roda.rodain.ui.utils.AutoscrollTreeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 28-09-2015.
 */
public class SchemaPane extends BorderPane {
  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaPane.class.getName());

  private TreeView<String> treeView;
  private VBox treeBox;
  private SchemaNode rootNode;
  private HBox topBox, bottom;
  private VBox dropBox;
  private static Stage primaryStage;
  private Set<SchemaNode> schemaNodes;

  // center help
  private VBox centerHelp;
  private BooleanProperty hasClassificationScheme;

  /**
   * Creates a new SchemaPane object.
   *
   * @param stage
   *          The primary stage of the application.
   */
  public SchemaPane(Stage stage) {
    super();
    setPadding(new Insets(10, 10, 0, 10));
    primaryStage = stage;
    schemaNodes = new HashSet<>();

    createTreeView();
    createTop();
    createBottom();

    createCenterHelp();
    this.setCenter(centerHelp);
    this.setTop(topBox);

    hasClassificationScheme = new SimpleBooleanProperty(false);

    String lastClassScheme = ConfigurationManager.getAppConfig(Constants.CONF_K_APP_LAST_CLASS_SCHEME);
    if (lastClassScheme != null && !"".equals(lastClassScheme)) {
      try {
        ClassificationSchema schema = Controller.loadClassificationSchemaFile(lastClassScheme);
        updateClassificationSchema(schema, true);
      } catch (IOException e) {
        LOGGER.error("Error reading classification scheme specification", e);
      }
    }

    this.prefWidthProperty().bind(stage.widthProperty().multiply(0.33));
    this.minWidthProperty().bind(stage.widthProperty().multiply(0.2));
  }

  public SchemaNode getRootNode() {
    return rootNode;
  }

  private void createTop() {
    Label title = new Label(I18n.t(Constants.I18N_SCHEMAPANE_TITLE).toUpperCase());
    title.getStyleClass().add(Constants.CSS_TITLE);

    topBox = new HBox();
    topBox.getStyleClass().add(Constants.CSS_TITLE_BOX);
    topBox.setPadding(new Insets(15, 15, 15, 15));
    topBox.setAlignment(Pos.CENTER_LEFT);
    topBox.getChildren().add(title);

    if (Boolean.parseBoolean(ConfigurationManager.getAppConfig(Constants.CONF_K_APP_HELP_ENABLED))) {
      Tooltip.install(topBox, new Tooltip(I18n.help("tooltip.schemaPane")));
    }
  }

  private void createCenterHelp() {
    centerHelp = new VBox();
    centerHelp.setPadding(new Insets(0, 10, 0, 10));
    VBox.setVgrow(centerHelp, Priority.ALWAYS);
    centerHelp.setAlignment(Pos.CENTER);

    VBox box = new VBox(40);
    box.setPadding(new Insets(22, 10, 10, 10));
    box.setMaxWidth(355);
    box.setMaxHeight(200);
    box.setMinHeight(200);

    createDropBox();

    HBox titleBox = new HBox();
    titleBox.setAlignment(Pos.CENTER);
    Label title = new Label(I18n.t(Constants.I18N_SCHEMAPANE_HELP_TITLE));
    title.getStyleClass().add(Constants.CSS_HELPTITLE);
    title.setTextAlignment(TextAlignment.CENTER);
    titleBox.getChildren().addAll(title);

    HBox loadBox = new HBox();
    loadBox.setAlignment(Pos.CENTER);
    Button load = new Button(I18n.t(Constants.I18N_LOAD));
    load.setMinHeight(65);
    load.setMinWidth(130);
    load.setMaxWidth(130);
    load.setOnAction(event -> loadClassificationSchema());
    load.getStyleClass().add(Constants.CSS_HELPBUTTON);
    loadBox.getChildren().add(load);

    if (Boolean.parseBoolean(ConfigurationManager.getAppConfig(Constants.CONF_K_APP_HELP_ENABLED))) {
      Tooltip.install(load, new Tooltip(I18n.help("tooltip.secondStep")));
    }

    Hyperlink link = new Hyperlink(I18n.t(Constants.I18N_SCHEMAPANE_CREATE));
    link.setOnAction(event -> createClassificationScheme());

    TextFlow flow = new TextFlow(new Text(I18n.t(Constants.I18N_SCHEMAPANE_OR)), link);
    flow.setTextAlignment(TextAlignment.CENTER);

    box.getChildren().addAll(titleBox, loadBox);
    centerHelp.getChildren().addAll(box, flow);
  }

  private void createDropBox() {
    dropBox = new VBox();
    dropBox.setId("schemaPaneDropBox");

    HBox innerBox = new HBox();
    VBox.setVgrow(innerBox, Priority.ALWAYS);
    innerBox.setAlignment(Pos.CENTER);
    innerBox.setMinHeight(200);

    Separator separatorTop = new Separator();
    Separator separatorBottom = new Separator();

    Label title = new Label(I18n.t(Constants.I18N_SCHEMAPANE_DRAG_HELP));
    title.getStyleClass().add(Constants.CSS_HELPTITLE);
    title.setTextAlignment(TextAlignment.CENTER);
    innerBox.getChildren().add(title);
    dropBox.getChildren().addAll(separatorTop, innerBox, separatorBottom);

    dropBox.setOnDragOver(event -> {
      if (rootNode != null && event.getGestureSource() instanceof SourceTreeCell) {
        event.acceptTransferModes(TransferMode.COPY);
        title.setText(I18n.t(Constants.I18N_INSPECTIONPANE_ON_DROP));
      }
      event.consume();
    });

    dropBox.setOnDragDropped(event -> {
      RodaInApplication.getSchemePane().startAssociation(rootNode);
      event.consume();
    });

    dropBox.setOnDragExited(event -> {
      title.setText(I18n.t(Constants.I18N_SCHEMAPANE_DRAG_HELP));
      event.consume();
    });
  }

  private void createTreeView() {
    // create tree pane
    treeBox = new VBox();
    treeBox.setPadding(new Insets(10, 0, 0, 0));
    VBox.setVgrow(treeBox, Priority.ALWAYS);

    createRootNode();

    // create the tree view
    treeView = new AutoscrollTreeView<>(rootNode);
    treeView.getStyleClass().add(Constants.CSS_MAIN_TREE);
    treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    VBox.setVgrow(treeView, Priority.ALWAYS);
    treeView.setShowRoot(false);
    treeView.setEditable(true);
    treeView.setCellFactory(param -> {
      SchemaTreeCell cell = new SchemaTreeCell();
      setDropEvent(cell);
      return cell;
    });

    Separator separatorBottom = new Separator();
    // add everything to the tree pane
    treeBox.getChildren().addAll(treeView, separatorBottom);
    treeView.setOnMouseClicked(new SchemaClickedEventHandler(this.getTreeView()));
    treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem>() {
      @Override
      public void changed(ObservableValue observable, TreeItem oldValue, TreeItem newValue) {
        RodaInApplication.getInspectionPane().saveMetadata();
        if (oldValue instanceof SipPreviewNode) {
          oldValue.valueProperty().unbind();
          ((SipPreviewNode) oldValue).setBlackIconSelected(true);
          forceUpdate(oldValue);
        }
        if (oldValue instanceof SchemaNode) {
          oldValue.valueProperty().unbind();
          ((SchemaNode) oldValue).setBlackIconSelected(true);
          forceUpdate(oldValue);
        }

        if (newValue != null) {
          List<TreeItem<String>> selectedItems = treeView.getSelectionModel().getSelectedItems();
          if (selectedItems.size() == 1) {
            // only one item is selected
            if (newValue instanceof SipPreviewNode) {
              ((SipPreviewNode) newValue).setBlackIconSelected(false);
              forceUpdate(newValue);
              RodaInApplication.getInspectionPane().update((SipPreviewNode) newValue);
            }
            if (newValue instanceof SchemaNode) {
              ((SchemaNode) newValue).setBlackIconSelected(false);
              forceUpdate(newValue);
              RodaInApplication.getInspectionPane().update((SchemaNode) newValue);
            }
            SchemeItemToString sits = new SchemeItemToString(treeView.getSelectionModel().getSelectedItems());
            sits.createAndUpdateFooter();
          } else {
            // more than one item is selected
            String multipleEditMax = ConfigurationManager.getAppConfig(Constants.CONF_K_APP_MULTIPLE_EDIT_MAX);
            int multipleEditMaxInt = 100;
            if (multipleEditMax != null) {
              multipleEditMaxInt = Integer.parseInt(multipleEditMax);
            }
            if (selectedItems.size() < multipleEditMaxInt) {
              RodaInApplication.getInspectionPane().update(selectedItems);
            } else {
              RodaInApplication.getInspectionPane().resetTop();
              RodaInApplication.getInspectionPane().setCenter(new HBox());
              Alert dlg = new Alert(Alert.AlertType.WARNING);
              dlg.initStyle(StageStyle.UNDECORATED);
              dlg.setHeaderText(I18n.t(Constants.I18N_SCHEMAPANE_TOO_MANY_SELECTED_HEADER));
              dlg.setTitle(I18n.t(Constants.I18N_SCHEMAPANE_TOO_MANY_SELECTED_TITLE));
              dlg.setContentText(I18n.t(Constants.I18N_SCHEMAPANE_TOO_MANY_SELECTED_CONTENT));
              dlg.initModality(Modality.APPLICATION_MODAL);
              dlg.initOwner(primaryStage);
              dlg.show();
            }
          }
        } else {
          Footer.setClassPlanStatus("");
        }
      }
    });
  }

  private void createRootNode() {
    Sip dobj = new Sip(DescriptiveMetadata.buildDefaultDescObjMetadata());
    dobj.setParentId(null);
    dobj.setId(null);
    rootNode = new SchemaNode(dobj);
    rootNode.setExpanded(true);
    rootNode.getChildren().addListener((ListChangeListener<? super TreeItem<String>>) c -> {
      if (rootNode.getChildren().isEmpty()) {
        setCenter(dropBox);
        RodaInApplication.getInspectionPane().showHelp();
      } else {
        setCenter(treeBox);
      }
    });
  }

  private void forceUpdate(TreeItem<String> item) {
    String value = item.getValue();
    item.setValue(null);
    item.setValue(value);
  }

  private TreeItem<String> getSelectedItem() {
    TreeItem<String> result = null;
    int selIndex = treeView.getSelectionModel().getSelectedIndex();
    if (selIndex != -1) {
      result = treeView.getTreeItem(selIndex);
    }
    return result;
  }

  /**
   * Creates a file chooser dialog so that the user can choose the
   * classification scheme file to be loaded. Then, loads the file and creates
   * the tree.
   */
  public void loadClassificationSchema() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle(I18n.t(Constants.I18N_FILE_CHOOSER_TITLE));
    File selectedFile = chooser.showOpenDialog(primaryStage);
    if (selectedFile == null)
      return;
    String inputFile = selectedFile.toPath().toString();
    try {
      ClassificationSchema schema = Controller.loadClassificationSchemaFile(inputFile);
      updateClassificationSchema(schema);
    } catch (IOException e) {
      LOGGER.error("Error reading classification scheme specification", e);
    }
  }

  public void updateClassificationSchema(ClassificationSchema cs) {
    updateClassificationSchema(cs, false);
  }

  private void updateClassificationSchema(ClassificationSchema cs, boolean skipConfirmation) {

    initializeTemplates(cs);
    if (!skipConfirmation && !confirmUpdate())
      return;

    setTop(topBox);
    setCenter(treeBox);
    setBottom(bottom);
    rootNode.getChildren().clear();
    List<Sip> dos = cs.getDos();
    Map<String, SchemaNode> nodes = new HashMap<>();
    Set<SchemaNode> roots = new HashSet<>();

    try {
      for (Sip descObj : dos) {
        // Check if the node is a root node
        if (descObj.getParentId() == null) {
          // Create a new node if it hasn't been created
          if (!nodes.containsKey(descObj.getId())) {
            SchemaNode root = new SchemaNode(descObj);
            root.setUpdateSIP(true);
            nodes.put(descObj.getId(), root);
          }
          roots.add(nodes.get(descObj.getId()));
        } else {
          // Get a list with the items where the id equals the node's parent's
          // id
          List<Sip> parents = dos.stream().filter(p -> descObj.getParentId().equals(p.getId()))
            .collect(Collectors.toList());
          // If the input file is well formed, there should be one item in the
          // list, no more and no less
          if (parents.size() != 1) {
            String format = "The node \"%s\" has %d parents";
            String message = String.format(format, descObj.getTitle(), parents.size());
            LOGGER.info("Error creating the scheme tree", new MalformedSchemaException(message));
            continue;
          }
          Sip parent = parents.get(0);
          SchemaNode parentNode;
          // If the parent node hasn't been processed yet, add it to the nodes
          // map
          if (nodes.containsKey(parent.getId())) {
            parentNode = nodes.get(parent.getId());
          } else {
            parentNode = new SchemaNode(parent);
            parentNode.setUpdateSIP(true);
            nodes.put(parent.getId(), parentNode);
          }
          SchemaNode node;
          // If the node hasn't been added yet, create it and add it to the
          // nodes map
          if (nodes.containsKey(descObj.getId())) {
            node = nodes.get(descObj.getId());
          } else {
            node = new SchemaNode(descObj);
            node.setUpdateSIP(true);
            nodes.put(descObj.getId(), node);
          }
          parentNode.getChildren().add(node);
          parentNode.addChildrenNode(node);
        }
      }

      // Add all the root nodes as children of the hidden rootNode
      for (SchemaNode sn : roots) {
        rootNode.getChildren().add(sn);
        schemaNodes.add(sn);
      }
      // if there were no nodes in the file, show the help panel
      if (roots.isEmpty()) {
        setTop(topBox);
        setCenter(centerHelp);
        setBottom(new HBox());
      } else {
        sortRootChildren();
        Platform.runLater(() -> {
          for (TreeItem<String> stringTreeItem : rootNode.getChildren()) {
            forceUpdate(stringTreeItem);
          }
        });
        hasClassificationScheme.setValue(true);
      }
    } catch (Exception e) {
      LOGGER.error("Error updating the classification plan", e);
    }
  }

  private void initializeTemplates(ClassificationSchema cs) {
    if (cs.getDos() != null) {
      for (Sip d : cs.getDos()) {
        if (d.getMetadata() != null) {
          for (DescriptiveMetadata dm : d.getMetadata()) {
            dm = Controller.updateTemplate(dm);
          }
        }
      }
    }

  }

  private boolean confirmUpdate() {
    if (rootNode.getChildren().isEmpty()) {
      return true;
    }
    String content = I18n.t(Constants.I18N_SCHEMAPANE_CONFIRM_NEW_SCHEME_CONTENT);
    Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
    dlg.initStyle(StageStyle.UNDECORATED);
    dlg.setHeaderText(I18n.t(Constants.I18N_SCHEMAPANE_CONFIRM_NEW_SCHEME_HEADER));
    dlg.setTitle(I18n.t(Constants.I18N_SCHEMAPANE_CONFIRM_NEW_SCHEME_TITLE));
    dlg.setContentText(content);
    dlg.initModality(Modality.APPLICATION_MODAL);
    dlg.initOwner(primaryStage);
    dlg.showAndWait();

    if (dlg.getResult().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
      rootNode.remove();
      createRootNode();
      treeView.setRoot(rootNode);
      return true;
    } else
      return false;
  }

  private void sortRootChildren() {
    ArrayList<TreeItem<String>> aux = new ArrayList<>(rootNode.getChildren());
    Collections.sort(aux, new SchemaComparator());
    rootNode.getChildren().setAll(aux);
  }

  private void createBottom() {
    bottom = new HBox(10);
    bottom.setPadding(new Insets(10, 10, 10, 10));
    bottom.setAlignment(Pos.CENTER);

    Button removeLevel = new Button(I18n.t(Constants.I18N_SCHEMAPANE_REMOVE));
    removeLevel.setId(Constants.CSS_REMOVE_LEVEL);
    removeLevel.setMinWidth(100);
    removeLevel.setOnAction(event -> {
      List<TreeItem<String>> selectedItems = new ArrayList<>(treeView.getSelectionModel().getSelectedItems());
      Alert dlg = new Alert(Alert.AlertType.CONFIRMATION);
      dlg.initStyle(StageStyle.UNDECORATED);
      dlg.setHeaderText(I18n.t(Constants.I18N_SCHEMAPANE_CONFIRM_REMOVE_HEADER));
      dlg.setTitle(I18n.t(Constants.I18N_SCHEMAPANE_CONFIRM_REMOVE_TITLE));
      dlg.setContentText(I18n.t(Constants.I18N_SCHEMAPANE_CONFIRM_REMOVE_CONTENT));
      dlg.initModality(Modality.APPLICATION_MODAL);
      dlg.initOwner(primaryStage);
      dlg.show();
      dlg.resultProperty().addListener(o -> confirmRemove(selectedItems, dlg.getResult()));
    });

    Button addLevel = new Button(I18n.t(Constants.I18N_SCHEMAPANE_ADD));
    addLevel.setMinWidth(100);

    addLevel.setOnAction(event -> addNewLevel());

    HBox space = new HBox();
    HBox.setHgrow(space, Priority.ALWAYS);

    bottom.getChildren().addAll(addLevel, removeLevel, space);
  }

  private void confirmRemove(List<TreeItem<String>> selectedItems, ButtonType type) {
    if (type.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
      treeView.getSelectionModel().clearSelection();
      Set<SipPreview> sipNodes = new HashSet<>();
      Set<SipPreview> fromRules = new HashSet<>();
      for (TreeItem<String> selected : selectedItems) {
        if (selected instanceof SipPreviewNode) {
          SipPreview currentSIP = ((SipPreviewNode) selected).getSip();
          sipNodes.add(currentSIP);
        }
        if (selected instanceof SchemaNode) {
          for (Rule r : ((SchemaNode) selected).getRules()) {
            fromRules.addAll(r.getSips());
          }
          // remove all the rules under this SchemaNode
          ((SchemaNode) selected).remove();
          // schema nodes need to be removed right away, but SIPs are only
          // removed after the remove task is complete
          removeNode(selected);
        }
      }

      sipNodes.removeAll(fromRules);

      for (SipPreview currentSIP : sipNodes) {
        RuleModalController.removeSipPreview(currentSIP);
        Task<Void> removeTask = new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            currentSIP.removeSIP();
            return null;
          }
        };

        // remove the nodes from the tree
        removeTask.setOnSucceeded(event -> selectedItems.forEach(this::removeNode));

        new Thread(removeTask).start();
      }
    }
  }

  private void removeNode(TreeItem<String> selected) {
    TreeItem parent = selected.getParent();
    if (parent != null) {
      if (parent instanceof SchemaNode) {
        ((SchemaNode) parent).removeChild(selected);
        parent.getChildren().remove(selected);
      } else {
        parent.getChildren().remove(selected);
      }
    }

    schemaNodes.remove(selected);
    treeView.getSelectionModel().clearSelection();
  }

  /**
   * Creates a new classification scheme and updates the UI.
   */
  public void createClassificationScheme() {
    if (!confirmUpdate()) {
      return;
    }
    setTop(topBox);
    setCenter(dropBox);
    setBottom(bottom);
    rootNode.getChildren().clear();
    schemaNodes.clear();
    hasClassificationScheme.setValue(true);
    ConfigurationManager.setAppConfig(Constants.CONF_K_APP_LAST_CLASS_SCHEME, "", true);
  }

  private SchemaNode addNewLevel() {
    TreeItem<String> selectedItem = getSelectedItem();
    SchemaNode selected = null;
    if (selectedItem instanceof SchemaNode) {
      selected = (SchemaNode) selectedItem;
    }
    ModalStage modalStage = new ModalStage(primaryStage);
    AddMetadataPane addMetadataPane = new AddMetadataPane(modalStage, null);
    modalStage.setRoot(addMetadataPane, true);
    if (addMetadataPane.getMetadataToAdd() != null) {
      Sip dobj = new Sip(addMetadataPane.getMetadataToAdd());

      dobj.setId(Controller.createID());
      dobj.setTitle(I18n.t(Constants.I18N_SCHEMAPANE_NEW_NODE));
      try {
        String metadataAggregationLevel = ConfigurationManager
          .getMetadataConfig(dobj.getMetadata().get(0).getTemplateType() + Constants.CONF_K_SUFFIX_AGGREG_LEVEL);
        dobj.setDescriptionlevel(metadataAggregationLevel);
      } catch (Throwable t) {
        LOGGER.error(t.getMessage(), t);
      }

      SchemaNode newNode = new SchemaNode(dobj);
      if (selected != null) {
        dobj.setParentId(selected.getDob().getId());
        selected.getChildren().add(newNode);
        selected.addChildrenNode(newNode);
        selected.sortChildren();
        if (!selected.isExpanded())
          selected.setExpanded(true);
      } else {
        try {
          String metadataTopLevel = ConfigurationManager
            .getMetadataConfig(dobj.getMetadata().get(0).getTemplateType() + Constants.CONF_K_SUFFIX_TOP_LEVEL);
          newNode.updateDescriptionLevel(metadataTopLevel);
        } catch (Throwable t) {
          LOGGER.error(t.getMessage(), t);

        }
        rootNode.getChildren().add(newNode);
        rootNode.addChildrenNode(newNode);
        schemaNodes.add(newNode);
        sortRootChildren();
      }
      selectedItem = newNode;
      setCenter(treeBox);

      // Edit the node's title as soon as it's created
      treeView.layout();
      treeView.edit(newNode);
      treeView.getSelectionModel().clearSelection();
      treeView.getSelectionModel().select(newNode);
      treeView.scrollTo(treeView.getSelectionModel().getSelectedIndex());

      return newNode;
    } else {
      return null;
    }
  }

  /**
   * Starts the association process.
   * 
   * @see #startAssociation()
   */
  public void startAssociation() {
    if (hasClassificationScheme.get()) {
      TreeItem<String> selected = getSelectedItem();
      if (selected != null && selected instanceof SchemaNode)
        startAssociation((SchemaNode) selected);
      else
        startAssociation(rootNode);
    }
  }

  /**
   * Starts the association process by getting the currently selected items from
   * the file explorer and initializing the modal that creates the association
   * rule.
   * 
   * @param descObj
   */
  public void startAssociation(SchemaNode descObj) {
    Set<SourceTreeItem> sourceSet = RodaInApplication.getSourceSelectedItems();
    boolean valid = true;
    // both trees need to have 1 element selected
    if (sourceSet != null && !sourceSet.isEmpty() && descObj != null) {
      Set<SourceTreeItem> toRemove = new HashSet<>();
      for (SourceTreeItem source : sourceSet) {
        if (source.getState() != PathState.NORMAL) {
          toRemove.add(source);
          continue;
        }
        if (!(source instanceof SourceTreeDirectory || source instanceof SourceTreeFile)) {
          valid = false;
          break;
        }
      }
      sourceSet.removeAll(toRemove);
    } else
      valid = false;

    // we need to check the size again because we may have deleted some items in
    // the "for" loop
    if (sourceSet == null || sourceSet.isEmpty())
      valid = false;

    if (valid)
      RuleModalController.newAssociation(primaryStage, sourceSet, descObj);
  }

  private void setDropEvent(SchemaTreeCell cell) {
    setOnDragDetected(cell);
    setOnDragOver(cell);
    setOnDragEntered(cell);
    setOnDragExited(cell);
    setOnDragDropped(cell);
  }

  private void setOnDragDetected(SchemaTreeCell cell) {
    cell.setOnDragDetected(event -> {
      TreeItem item = cell.getTreeItem();
      Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
      ClipboardContent content = new ClipboardContent();
      String s = "";
      if (item instanceof SchemaNode) {
        s = "scheme node - " + ((SchemaNode) item).getDob().getId();
      } else if (item instanceof SipPreviewNode) {
        s = "sip preview - " + ((SipPreviewNode) item).getSip().getId();
      }
      content.putString(s);
      db.setContent(content);
      event.consume();
    });
  }

  private void setOnDragOver(final SchemaTreeCell cell) {
    // on a Target
    cell.setOnDragOver(event -> {
      TreeItem<String> treeItem = cell.getTreeItem();
      if (treeItem == null) {
        if (event.getGestureSource() instanceof SchemaNode)
          event.acceptTransferModes(TransferMode.MOVE);
        else
          event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      if (treeItem instanceof SchemaNode) {
        SchemaNode item = (SchemaNode) cell.getTreeItem();
        if (item != null && event.getGestureSource() != cell && event.getDragboard().hasString()) {
          event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
      }
      if (treeItem instanceof SipPreviewNode) {
        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
      }
      event.consume();
    });
  }

  private void setOnDragEntered(final SchemaTreeCell cell) {
    // on a Target
    cell.setOnDragEntered(event -> {
      TreeItem<String> treeItem = cell.getTreeItem();
      if (treeItem instanceof SchemaNode) {
        SchemaNode item = (SchemaNode) cell.getTreeItem();
        if (item != null && event.getGestureSource() != cell && event.getDragboard().hasString()) {
          cell.getStyleClass().add(Constants.CSS_SCHEMANODEHOVERED);
        }
      }
      event.consume();
    });
  }

  private void setOnDragExited(final SchemaTreeCell cell) {
    // on a Target
    cell.setOnDragExited(event -> {
      cell.getStyleClass().remove(Constants.CSS_SCHEMANODEHOVERED);
      cell.updateItem(cell.getItem(), false);
      event.consume();
    });
  }

  private void setOnDragDropped(final SchemaTreeCell cell) {
    // on a Target
    cell.setOnDragDropped(event -> {
      TreeItem treeItem = cell.getTreeItem();
      Dragboard db = event.getDragboard();
      boolean success = false;
      if (db.hasString()) {
        // edit the classification scheme
        if (db.getString().startsWith("scheme")) {
          TreeItem selected = treeView.getSelectionModel().getSelectedItem();
          SchemaNode node = (SchemaNode) treeItem;

          // If the target item is a descendant of the source item, they would
          // both disappear since there would be no remaining connection to the
          // rest of the tree
          if (checkTargetIsDescendant(selected, node)) {
            return;
          }

          TreeItem parent = selected.getParent();
          parent.getChildren().remove(selected);

          SchemaNode schemaNode = (SchemaNode) selected;
          if (node == null) {
            rootNode.getChildren().add(selected);
            schemaNode.getDob().setParentId(null);
            sortRootChildren();
          } else {
            node.getChildren().add(selected);
            schemaNode.getDob().setParentId(node.getDob().getId());
            node.sortChildren();
          }
        } else if (db.getString().startsWith("sip preview")) {
          SchemaNode target = null;
          if (treeItem instanceof SipPreviewNode) {
            target = (SchemaNode) treeItem.getParent();
          } else
            target = (SchemaNode) treeItem;
          if (target == null) {
            target = rootNode;
          }

          List<TreeItem<String>> selectedItems = new ArrayList<>(treeView.getSelectionModel().getSelectedItems());
          for (TreeItem t : selectedItems) {
            if (t instanceof SipPreviewNode) {
              SipPreviewNode sourceSIP = (SipPreviewNode) t;

              // Remove the SIP from its parent and rule
              SchemaNode parent = (SchemaNode) sourceSIP.getParent();
              parent.removeChild(sourceSIP);
              sourceSIP.getSip().removeFromRule();

              // Add the SIP to the new parent
              String newParentID = null;
              if (target != rootNode)
                newParentID = target.getDob().getId();
              sourceSIP.getSip().setParentId(newParentID);
              target.addChild(Controller.createID(), sourceSIP);
              target.getChildren().add(sourceSIP);
              target.sortChildren();
            }
          }
        } else {
          if (treeItem != null) {
            // dropped on a SIP, associate to the parent of the SIP
            if (treeItem instanceof SipPreviewNode) {
              SipPreviewNode sipPreviewNode = (SipPreviewNode) treeItem;
              startAssociation((SchemaNode) sipPreviewNode.getParent());
            } else {
              // normal association
              startAssociation((SchemaNode) treeItem);
            }
          } else {
            // association to the empty tree view
            startAssociation(rootNode);
          }
        }
        success = true;
      }
      event.setDropCompleted(success);
      event.consume();
    });
  }

  private boolean checkTargetIsDescendant(TreeItem source, TreeItem target) {
    if (target == null) {
      return false;
    }
    TreeItem aux = target.getParent();
    boolean isChild = false;
    while (aux != null) {
      if (aux == source) {
        isChild = true;
        break;
      }
      aux = aux.getParent();
    }
    return isChild;
  }

  private Set<SchemaNode> recursiveGetSchemaNodes(TreeItem<String> root) {
    Set<SchemaNode> result = new HashSet<>();
    for (TreeItem<String> t : root.getChildren()) {
      if (t instanceof SchemaNode) {
        result.add((SchemaNode) t);
      }
      result.addAll(recursiveGetSchemaNodes(t));
    }
    return result;
  }

  /**
   * Shows the plan tree.
   */
  public void showTree() {
    setCenter(treeBox);
  }

  /**
   * Shows the help panel.
   */
  public void showHelp() {
    rootNode.getChildren().clear();
    setTop(topBox);
    setCenter(centerHelp);
    setBottom(new HBox());
    hasClassificationScheme.setValue(false);
  }

  /**
   * @return A set with all the SchemaNodes in the tree
   */
  public Set<SchemaNode> getSchemaNodes() {
    return recursiveGetSchemaNodes(rootNode);
  }

  /**
   * @return The TreeView of the SchemaPane
   */
  public TreeView<String> getTreeView() {
    return treeView;
  }

  /**
   * @return The property which can be used to know if a classification scheme
   *         is present or not.
   */
  public BooleanProperty hasClassificationScheme() {
    return hasClassificationScheme;
  }

  /**
   * @return A map with all the description objects of the tree (not only the
   *         SIPs but also the hierarchy). Each key is a description object and
   *         each value is a list of that object's ancestors IDs.
   */
  public Map<Sip, List<String>> getAllDescriptionObjects() {
    Set<SchemaNode> localSchemaNodes = new HashSet<>(schemaNodes);
    localSchemaNodes.add(rootNode);
    Map<SipPreview, List<String>> sipsMap = new HashMap<>();
    Map<Sip, List<String>> descObjsMap = new HashMap<>();

    for (SchemaNode sn : localSchemaNodes) {
      sipsMap.putAll(sn.getSipPreviews());
      descObjsMap.putAll(sn.getDescriptionObjects());
    }
    // we don't want to return the root, since its a hidden node that is only
    // useful for presentation
    descObjsMap.remove(rootNode.getDob());
    // filter out the SIPs marked as "removed"
    sipsMap = sipsMap.entrySet().stream().parallel().filter(p -> !p.getKey().isRemoved())
      .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

    descObjsMap.putAll(sipsMap);
    return descObjsMap;
  }

  /**
   * @return A map with the selected description objects of the tree (not only
   *         the SIPs but also the hierarchy). Each key is a description object
   *         and each value is a list of that object's ancestors IDs.
   */
  public Map<Sip, List<String>> getSelectedDescriptionObjects() {
    Map<SipPreview, List<String>> sipsMap = new HashMap<>();
    Map<Sip, List<String>> descObjsMap = new HashMap<>();

    ObservableList<TreeItem<String>> selected = treeView.getSelectionModel().getSelectedItems();
    if (selected != null) {
      for (TreeItem<String> item : selected) {
        if (item instanceof SipPreviewNode) {
          SipPreviewNode sip = (SipPreviewNode) item;
          SchemaNode parent = (SchemaNode) sip.getParent();
          sipsMap.put(sip.getSip(), parent.computeAncestorsOfSips());
        }
        if (item instanceof SchemaNode) {
          SchemaNode itemNode = (SchemaNode) item;
          sipsMap.putAll(itemNode.getSipPreviews());
          descObjsMap.put(itemNode.getDob(), itemNode.computeAncestors());
        }
      }
    }
    if (sipsMap.isEmpty() && descObjsMap.isEmpty()) {
      // add all the SIPs to the result map
      descObjsMap = getAllDescriptionObjects();
    } else {
      // filter out the SIPs marked as "removed"
      sipsMap = sipsMap.entrySet().stream().parallel().filter(p -> !p.getKey().isRemoved())
        .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
      descObjsMap.putAll(sipsMap);
    }
    return descObjsMap;
  }

  public void forceUpdateSelectionIcons() {
    // clear selection and re-select
    List<Integer> selectedIndices = new ArrayList<>(treeView.getSelectionModel().getSelectedIndices());
    treeView.getSelectionModel().clearSelection();
    for (Integer selectedIndex : selectedIndices) {
      treeView.getSelectionModel().select(selectedIndex);
    }
  }
}
