package com.app.ui.treeview;

import com.api.Api;
import com.dto.CommandTreeNodeDto;
import com.dto.ProgramTreeDto;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.util.List;

public class ProgramTreeViewHelper {

    private static final CommandTreeNodeDto DUMMY = new CommandTreeNodeDto(-1L, List.of(), "", "", false, false, List.of());

    public TreeTableView<CommandTreeNodeDto> buildTree(ProgramTreeDto dto){
        TreeTableView<CommandTreeNodeDto> tree = new TreeTableView<>();
        tree.setShowRoot(false);

        TreeTableColumn<CommandTreeNodeDto, String> textCol = new TreeTableColumn<>("Command");
        textCol.setPrefWidth(600);
        textCol.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                param.getValue() != null && param.getValue().getValue() != null ? param.getValue().getValue().getText() : ""
        ));

        tree.getColumns().setAll(textCol);

        TreeItem<CommandTreeNodeDto> root = new TreeItem<>();
        for (CommandTreeNodeDto n : dto.getRoots()){
            root.getChildren().add(buildItem(n));
        }
        tree.setRoot(root);

        attachExpandCollapseHandlers(tree);
        return tree;
    }

    private TreeItem<CommandTreeNodeDto> buildItem(CommandTreeNodeDto node){
        TreeItem<CommandTreeNodeDto> item = new TreeItem<>(node){
            @Override
            public boolean isLeaf(){
                // Base commands are primitives (leaves). Synthetic (isBase=false) can expand.
                CommandTreeNodeDto v = getValue();
                return v == null || v.isBase();
            }
        };

        // Only synthetic nodes (isBase=false) are expandable
        if (!node.isBase()){
            if (node.isExpanded()){
                for (CommandTreeNodeDto c : node.getChildren()){
                    item.getChildren().add(buildItem(c));
                }
                item.setExpanded(true);
            } else {
                // add dummy so a disclosure node appears
                item.getChildren().add(new TreeItem<>(DUMMY));
                item.setExpanded(false);
            }
        }
        return item;
    }

    private void attachExpandCollapseHandlers(TreeTableView<CommandTreeNodeDto> tree){
        attachRecursively(tree, tree.getRoot());
    }

    private void attachRecursively(TreeTableView<CommandTreeNodeDto> tree, TreeItem<CommandTreeNodeDto> item){
        if (item == null) return;
        CommandTreeNodeDto node = item.getValue();
        if (node != null && node.isBase()){
            // base nodes are leaves; no handler
        } else if (node != null){
            item.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                // only synthetic nodes toggle
                try {
                    ProgramTreeDto refreshed = isNowExpanded
                            ? Api.expandMixedAt(node.getPath())
                            : Api.collapseMixedAt(node.getPath());
                    TreeItem<CommandTreeNodeDto> newRoot = new TreeItem<>();
                    for (CommandTreeNodeDto n : refreshed.getRoots()){
                        newRoot.getChildren().add(buildItem(n));
                    }
                    tree.setRoot(newRoot);
                    tree.setShowRoot(false);
                    attachExpandCollapseHandlers(tree);
                } catch (Exception ignored){
                }
            });
        }
        if (item.getChildren() != null){
            for (TreeItem<CommandTreeNodeDto> child : item.getChildren()){
                attachRecursively(tree, child);
            }
        }
    }
}


