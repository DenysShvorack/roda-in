package org.roda.rodain.core;

import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import org.roda.rodain.creation.ui.CreationModalPreparation;
import org.roda.rodain.schema.DescriptionObject;
import org.roda.rodain.schema.ui.SchemaNode;
import org.roda.rodain.schema.ui.SchemaPane;
import org.roda.rodain.schema.ui.SipPreviewNode;
import org.roda.rodain.source.ui.FileExplorerPane;
import org.roda.rodain.testing.Utils;
import org.testfx.framework.junit.ApplicationTest;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Andre Pereira apereira@keep.pt
 * @since 17-12-2015.
 */
public class MainTest extends ApplicationTest {
  private static Path testDir, output;
  private SchemaPane schemaPane;
  private FileExplorerPane fileExplorer;

  @Override
  public void start(Stage stage) throws Exception {
    RodaIn main = new RodaIn();
    main.start(stage);

    sleep(3000);

    schemaPane = RodaIn.getSchemePane();
    fileExplorer = RodaIn.getFileExplorer();

    Path path = Paths.get("src/test/resources/plan_with_errors.json");
    InputStream stream = new FileInputStream(path.toFile());
    schemaPane.loadClassificationSchemeFromStream(stream);
  }

  @Before
  public void setUpBeforeClass() throws Exception {
    testDir = Utils.createFolderStructure();
  }

  @Test
  public void newSchemePane() {
    sleep(5000);
    push(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
    sleep(3000);
    try {
      push(KeyCode.RIGHT);
      push(KeyCode.ENTER);
    } catch (Exception e) {
    }
    sleep(12000);
    clickOn(I18n.t("SchemaPane.add"));
    sleep(2000);
    clickOn(".schemaNode");
    sleep(1000);
    clickOn("#descObjTitle");
    eraseText(50);
    write("Node1");
    sleep(1000);

    TreeItem<String> item = RodaIn.getSchemePane().getTreeView().getSelectionModel().getSelectedItem();
    assert "Node1".equals(item.getValue());

    clickOn(I18n.t("SchemaPane.add"));
    sleep(500);
    clickOn(I18n.t("SchemaPane.newNode"));
    sleep(500);
    clickOn("#descObjTitle");
    eraseText(50);
    write("Node2");
    sleep(500);

    doubleClickOn(".tree-view");

    clickOn("Node2");

    TreeItem<String> newItem = RodaIn.getSchemePane().getTreeView().getSelectionModel().getSelectedItem();
    assert newItem instanceof SchemaNode;
    SchemaNode newNode = (SchemaNode) newItem;
    DescriptionObject dobj = newNode.getDob();
    assert dobj != null;
    assert dobj.getDescriptionlevel() != null;

    sleep(2000);
    drag("Node2").dropTo(".tree-view");
    assert RodaIn.getSchemePane().getTreeView().getRoot().getChildren().size() == 2;
    sleep(1000);
    clickOn("Node2").clickOn("#removeLevel");
    try {
      push(KeyCode.RIGHT);
      push(KeyCode.ENTER);
    } catch (Exception e) {
    }
    assert RodaIn.getSchemePane().getTreeView().getRoot().getChildren().size() == 1;
  }

  @Test
  public void schemePane() {
    sleep(5000); // wait for the classification scheme to load

    clickOn("UCP");
    TreeItem selected = schemaPane.getTreeView().getSelectionModel().getSelectedItem();
    int selectedIndex = schemaPane.getTreeView().getSelectionModel().getSelectedIndex();
    assert "UCP".equals(selected.getValue());
    assert selectedIndex == 7;

    doubleClickOn(".tree-view");

    doubleClickOn("UCP");

    assert selected.getChildren().size() == 1;
  }

  @Test
  public void association() {
    Platform.runLater(() -> fileExplorer.setFileExplorerRoot(testDir));

    sleep(5000); // wait for the tree to be created
    doubleClickOn("dir4");
    sleep(2000); // wait for the node to expand
    drag("dirB").dropTo("UCP");
    sleep(2000); // wait for the modal to open
    clickOn("#assoc3");
    clickOn("#btConfirm");
    sleep(2000); // wait for the modal to update
    clickOn("#btConfirm");
    sleep(6000); // wait for the SIPs creation

    clickOn("UCP");
    clickOn("file1.txt");
    TreeItem selected = schemaPane.getTreeView().getSelectionModel().getSelectedItem();
    assert selected instanceof SipPreviewNode;
    TreeItem parent = selected.getParent();
    assert parent instanceof SchemaNode;
    assert "UCP".equals(((SchemaNode) parent).getDob().getTitle());

    assert parent.getChildren().size() == 14;

    clickOn("#removeLevel");
    try {
      push(KeyCode.RIGHT);
      push(KeyCode.ENTER);
    } catch (Exception e) {
    }

    sleep(1000); // wait for the SIP removal
    assert parent.getChildren().size() == 13;

    clickOn("UCP");
    clickOn("#removeRule1");
    sleep(1000); // wait for the rule to be removed

    assert parent.getChildren().size() == 1;

    // create 2 SIPs
    clickOn("fileA.txt");
    press(KeyCode.CONTROL);
    clickOn("fileB.txt");
    release(KeyCode.CONTROL);

    drag().dropTo("UCP");
    sleep(1000); // wait for the modal to open
    clickOn("#assoc2");
    clickOn(I18n.t("continue"));
    sleep(1000); // wait for the modal to update
    clickOn("#meta4");
    clickOn(I18n.t("confirm"));
    sleep(5000); // wait for the SIPs creation

    clickOn(I18n.t("Main.file"));
    clickOn(I18n.t("Main.exportSips"));
    output = Utils.homeDir.resolve("SIPs output");
    output.toFile().mkdir();
    CreationModalPreparation.setOutputFolder(output.toString());
    clickOn(I18n.t("start"));

    sleep(5000);
    clickOn(I18n.t("close"));

    clickOn(I18n.t("Main.file"));
    clickOn(I18n.t("Main.exportSips"));
    clickOn("#sipTypes").clickOn("BagIt");
    CreationModalPreparation.setOutputFolder(output.toString());
    clickOn(I18n.t("start"));
    sleep(5000);
    clickOn(I18n.t("close"));

    clickOn("FTP");
    sleep(1000);

    clickOn("UCP");
    clickOn("#removeRule2");
    sleep(1000); // wait for the rule to be removed
  }
}
