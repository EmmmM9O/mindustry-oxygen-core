package com.github.emmmm9o.oxygencore.ui.layout;

import com.github.emmmm9o.oxygencore.util.Tree;
import com.github.emmmm9o.oxygencore.util.Tree.TreeNode;
import com.github.emmmm9o.oxygencore.ui.StyleManager;

import arc.func.Func;
import arc.math.Interp;
import arc.scene.Action;
import arc.scene.actions.Actions;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.gen.Tex;

/**
 * TreeTable
 */
public class TreeTable<E> extends Table {

  public Seq<Tree<E>> trees;

  public static class treeData {
    public ImageButton button;
    public boolean collage;
    public Collapser collapser;

    public treeData(ImageButton button, Collapser collapser, boolean collage) {
      this.button = button;
      this.collage = collage;
      this.collapser = collapser;
    }
  }

  public ObjectMap<TreeNode<E>, treeData> datas;

  public TreeTable(Seq<Tree<E>> trees) {
    this.trees = trees;
    datas = new ObjectMap<>();
    rebuildTree();
  }

  public TreeTable(Seq<E> datas, Func<E, E> getFather) {
    this(new Tree.FParsrr<E>(datas, getFather).parse());
  }

  public void drawNodeContent(E data, Table table) {

  }

  public void drawNode(TreeNode<E> node, Table table) {
    var data = datas.get(node, new treeData(null, null, true));
    table.table(top -> {
      if (node.children.size != 0) {
        ImageButton button = top.button(data.collage ? Icon.upSmall : Icon.downSmall, StyleManager.style.none, () -> {
          data.collage = !data.collage;
          data.button.getChildren().get(0).addAction(Actions.rotateBy(180f, 0.4f, Interp.fade));
          data.collapser.setCollapsed(data.collage);
        }).size(32f).left().uniformY().get();
        data.button = button;
        // datas.put(node, new treeData(button, data.collage));
      }
      top.table(content -> {
        drawNodeContent(node.data, content);
      }).uniformY().growX().height(32f).left();
    }).uniformX().growX().height(32f).row();
    if (node.children.size != 0) {
      data.collapser = table.add(new Collapser(listT -> {
        listT.table(pad -> {
        }).width(5f).uniformY().grow().left();
        listT.table(list -> {
          for (var child : node.children) {
            list.table(element -> {
              drawNode(child, element);
            }).uniformX().growX().row();
          }
        }).grow().uniformY().right();
      }, data.collage)).uniformX().grow().get();

    }
  }

  public void rebuildTree() {
    this.clearChildren();
    pane(table -> {
      for (var tree : trees) {
        table.table(Tex.pane, treeT -> {
          drawNode(tree.root, treeT);
        }).uniformX().grow();
      }
    });
  }
}
