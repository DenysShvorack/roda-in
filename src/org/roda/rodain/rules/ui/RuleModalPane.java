package rodain.rules.ui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rodain.rules.MetadataTypes;
import rodain.rules.RuleTypes;
import rodain.schema.ui.SchemaNode;
import rodain.source.ui.items.SourceTreeDirectory;
import rodain.source.ui.items.SourceTreeFile;
import rodain.source.ui.items.SourceTreeItem;
import rodain.utils.Utils;

/**
 * Created by adrapereira on 28-09-2015.
 */
public class RuleModalPane extends BorderPane {
    private enum States {
        ASSOCIATION,
        METADATA
    }
    private Stage stage;
    private SchemaNode schema;
    private Set<SourceTreeItem> sourceSet;
    //Association
    private GridPane gridAssociation;
    private ToggleGroup groupAssoc;
    private ComboBox<Integer> level;
    //Metadata
    private VBox gridMetadata;
    private ToggleGroup groupMetadata;
    private RadioButton diffFolder, sameFolder;
    private Button chooseDir, chooseFile;

    private Button btContinue, btCancel;
    private States currentState;
    private String fromFile, diffDir, sameDir;


    public RuleModalPane(Stage stage, Set<SourceTreeItem> sourceSet, SchemaNode schemaNode){
        super();
        schema = schemaNode;
        this.sourceSet = sourceSet;
        this.stage = stage;
        setStyle("-fx-border-color: lightgray; -fx-border-width: 2px; -fx-background-color: white;");

        createTop();
        createCenter();
        createBottom();

        currentState = States.ASSOCIATION;
    }

    private void createTop(){
        StackPane pane = new StackPane();
        pane.setPadding(new Insets(0, 0, 10, 0));

        HBox hbox = new HBox();
        HBox.setHgrow(hbox, Priority.ALWAYS);
        hbox.setStyle("-fx-background-color: lightgray;");
        hbox.setPadding(new Insets(5, 5, 5, 5));
        pane.getChildren().add(hbox);

        Label source = new Label();
        StringBuilder sb = new StringBuilder();
        for(SourceTreeItem it: sourceSet) {
            if(it instanceof SourceTreeDirectory)
                sb.append(((SourceTreeDirectory) it).getValue()).append(",");
            else if(it instanceof SourceTreeFile)
                sb.append(((SourceTreeFile)it).getValue()).append(",");
        }
        //remove the last comma
        int lastComma = sb.lastIndexOf(",");
        sb.replace(lastComma, lastComma + 1,"");
        source.setText(sb.toString());

        source.setMinHeight(24);
        source.setId("title");
        source.setGraphic(new ImageView(SourceTreeDirectory.folderCollapseImage));
        source.setStyle(" -fx-text-fill: black");
        source.setWrapText(true);

        Label descObj = new Label(schema.getDob().getTitle());
        descObj.setMinHeight(24);
        descObj.setId("title");
        descObj.setGraphic(new ImageView(schema.getImage()));
        descObj.setTextAlignment(TextAlignment.LEFT);
        descObj.setStyle(" -fx-text-fill: black");

        HBox space = new HBox();
        HBox.setHgrow(space, Priority.ALWAYS);
        space.setMinWidth(50);

        hbox.getChildren().addAll(source, space, descObj);

        setTop(pane);
    }

    private void createCenter(){
        gridAssociation = createCenterAssociation();
        gridMetadata = createCenterMetadata();

        setCenter(gridAssociation);
    }

    private GridPane createCenterAssociation(){
        GridPane gridCenter = new GridPane();
        gridCenter.setPadding(new Insets(0, 10, 0, 10));
        gridCenter.setVgap(5);
        gridCenter.setHgap(10);

        groupAssoc = new ToggleGroup();

        RadioButton singleSip = new RadioButton("Create a single SIP");
        singleSip.setToggleGroup(groupAssoc);
        singleSip.setUserData(RuleTypes.SINGLESIP);
        singleSip.setSelected(true);
        singleSip.setStyle(" -fx-text-fill: black");

        RadioButton perFile = new RadioButton("Create one SIP per file");
        perFile.setToggleGroup(groupAssoc);
        perFile.setUserData(RuleTypes.SIPPERFILE);
        perFile.setStyle(" -fx-text-fill: black");

        //count number of folders selected in the source
        int dirCount = 0;
        for(SourceTreeItem sti: sourceSet)
            if(sti instanceof SourceTreeDirectory)
                dirCount++;

        RadioButton byFolder = new RadioButton("Create one SIP per folder until level");
        byFolder.setUserData(RuleTypes.SIPPERFOLDER);
        byFolder.setToggleGroup(groupAssoc);
        byFolder.setStyle(" -fx-text-fill: black");

        int depth = 0;
        if(dirCount == sourceSet.size()) { //check if the selected items are all directories
            //we only need to compute the depth if we'll be going to activate the radio button
            for (SourceTreeItem std : sourceSet) {
                Path startPath = Paths.get(std.getPath());
                int depthAux = Utils.getRelativeMaxDepth(startPath);
                if (depthAux > depth)
                    depth = depthAux;
            }
        }else byFolder.setDisable(true); //disable this option because it doesn't make sense if all the top level items aren't directories

        ArrayList<Integer> levels = new ArrayList<>();
        for (int i = 1; i <= depth; i++)
            levels.add(i);

        ObservableList<Integer> options = FXCollections.observableArrayList(levels);
        level = new ComboBox<>(options);
        level.setValue((int)Math.ceil(depth/2.0));

        if(dirCount != sourceSet.size()) //disable the level comboBox too if this option doesn't make sense
            level.setDisable(true);

        gridCenter.add(singleSip, 0, 1);
        gridCenter.add(perFile, 0, 2);
        gridCenter.add(byFolder, 0, 3);
        gridCenter.add(level, 1, 3);

        return gridCenter;
    }

    private VBox createCenterMetadata(){
        VBox gridCenter = new VBox(10);
        gridCenter.setPadding(new Insets(0, 10, 0, 10));

        Label metaTitle = new Label("Apply metadata from:");
        metaTitle.setId("title");

        VBox center = new VBox(5);
        groupMetadata = new ToggleGroup();

        RadioButton newFile = new RadioButton("An empty text area");
        newFile.setUserData(MetadataTypes.NEWTEXT);
        newFile.setToggleGroup(groupMetadata);
        newFile.setStyle(" -fx-text-fill: black");

        rbSingleFile(center);
        rbSameFolder(center);
        rbDiffFolder(center);
        center.getChildren().add(newFile);

        gridCenter.getChildren().addAll(metaTitle, center);

        return gridCenter;
    }

    private void rbSingleFile(VBox gridCenter){
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);

        final RadioButton singleFile = new RadioButton("A single file");
        singleFile.setToggleGroup(groupMetadata);
        singleFile.setUserData(MetadataTypes.SINGLEFILE);
        singleFile.setSelected(true);
        singleFile.setStyle(" -fx-text-fill: black");

        chooseFile = new Button("Choose File...");
        chooseFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                singleFile.setSelected(true);
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Please choose a file");
                File selectedFile = chooser.showOpenDialog(stage);
                if (selectedFile == null)
                    return;
                fromFile = selectedFile.toPath().toString();
                chooseFile.setText(selectedFile.toPath().getFileName().toString());
                chooseFile.setUserData(fromFile);
            }
        });

        HBox space = new HBox();
        HBox.setHgrow(space, Priority.ALWAYS);

        hbox.getChildren().addAll(singleFile, space, chooseFile);
        gridCenter.getChildren().add(hbox);
    }

    private void rbSameFolder(VBox gridCenter){
        //fill list with the directories in the source set
        List<String> directories = new ArrayList<>();
        for(SourceTreeItem sti: sourceSet){
            if(sti instanceof SourceTreeDirectory)
                directories.add(sti.getPath());
            else{ //if the item isn't a directory, get its parent
                Path path = Paths.get(sti.getPath());
                directories.add(path.getParent().toString());
            }
        }
        //get the common prefix of the directories
        sameDir = Utils.longestCommonPrefix(directories);

        sameFolder = new RadioButton("The directory \"" + sameDir + "\"");
        sameFolder.setToggleGroup(groupMetadata);
        sameFolder.setUserData(MetadataTypes.SAMEDIRECTORY);
        sameFolder.setStyle(" -fx-text-fill: black");

        gridCenter.getChildren().add(sameFolder);
    }

    private void rbDiffFolder(VBox gridCenter){
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);

        diffFolder = new RadioButton("Another directory");
        diffFolder.setUserData(MetadataTypes.DIFFDIRECTORY);
        diffFolder.setToggleGroup(groupMetadata);
        diffFolder.setStyle(" -fx-text-fill: black");

        chooseDir = new Button("Choose Directory...");
        chooseDir.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                diffFolder.setSelected(true);
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Please choose a folder");
                File selectedDirectory = chooser.showDialog(stage);
                if (selectedDirectory == null)
                    return;
                diffDir = selectedDirectory.toPath().toString();
                chooseDir.setText(selectedDirectory.toPath().getFileName().toString());
                chooseDir.setUserData(diffDir);
            }
        });

        HBox space = new HBox();
        HBox.setHgrow(space, Priority.ALWAYS);

        hbox.getChildren().addAll(diffFolder, space, chooseDir);
        gridCenter.getChildren().add(hbox);
    }

    private void createBottom(){
        btCancel = new Button("Cancel");
        btContinue = new Button("Continue");
        Label lState = new Label("");
        VBox.setVgrow(lState, Priority.ALWAYS);
        lState.setAlignment(Pos.BOTTOM_CENTER);
        lState.setStyle(" -fx-text-fill: darkgrey");

        HBox space = new HBox();
        HBox.setHgrow(space, Priority.ALWAYS);

        HBox buttons = new HBox();
        buttons.setPadding(new Insets(10, 10, 10, 10));
        buttons.setSpacing(10);
        buttons.setAlignment(Pos.CENTER);
        buttons.getChildren().addAll(btCancel, space, lState, btContinue);


        btContinue.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                if(currentState == States.ASSOCIATION) {
                    setCenter(gridMetadata);
                    currentState = States.METADATA;
                    enableMetaRadioButtons();
                }else if(currentState == States.METADATA) {
                    if(metadataCheckContinue())
                        RuleModalController.confirm();
                }
            }
        });

        btCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                RuleModalController.cancel();
            }
        });

        setBottom(buttons);
    }

    private boolean metadataCheckContinue(){
        try {
            MetadataTypes metaType = getMetadataType();
            if(metaType == MetadataTypes.SINGLEFILE)
                return chooseFile.getUserData() != null;
            if(metaType == MetadataTypes.DIFFDIRECTORY)
                return chooseDir.getUserData() != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void enableMetaRadioButtons(){
        try {
            RuleTypes assocType = getAssociationType();
            if(assocType == RuleTypes.SINGLESIP){
                sameFolder.setDisable(true);
                diffFolder.setDisable(true);
                chooseDir.setDisable(true);
            }else {
                sameFolder.setDisable(false);
                diffFolder.setDisable(false);
                chooseDir.setDisable(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<SourceTreeItem> getSourceSet(){
        return sourceSet;
    }

    public RuleTypes getAssociationType() throws Exception{
        Toggle selected = groupAssoc.getSelectedToggle();
        if(selected.getUserData() instanceof RuleTypes)
            return (RuleTypes)selected.getUserData();
        else throw new Exception("Unexpected user data type.");
    }

    public int getLevel(){
        return level.getValue();
    }

    public MetadataTypes getMetadataType() throws Exception{
        Toggle selected = groupMetadata.getSelectedToggle();
        if(selected.getUserData() instanceof MetadataTypes)
            return (MetadataTypes)selected.getUserData();
        else throw new Exception("Unexpected user data type.");
    }

    public String getFromFile() {
        return fromFile;
    }

    public String getDiffDir() {
        return diffDir;
    }

    public String getSameDir() {
        return sameDir;
    }
}
