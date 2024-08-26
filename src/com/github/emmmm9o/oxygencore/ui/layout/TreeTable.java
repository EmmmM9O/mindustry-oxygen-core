package com.github.emmmm9o.oxygencore.ui.layout;

import com.github.emmmm9o.oxygencore.util.Tree;
import com.github.emmmm9o.oxygencore.util.Tree.TreeNode;
import com.github.emmmm9o.oxygencore.ui.StyleManager;

import arc.func.Func;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Tex;

/**
 * TreeTable
 */
public class TreeTable<E> extends Table {

  public Seq<Tree<E>> trees;

  public TreeTable(Seq<Tree<E>> trees) {
    this.trees = trees;
    rebuildTree();
  }

  public TreeTable(Seq<E> datas, Func<E, E> getFather) {
    this(new Tree.FParsrr<E>(datas, getFather).parse());
  }

  public void drawNodeContent(E data, Table table) {

  }

  public void drawNode(TreeNode<E> node, Table table) {
    table.table(content -> {
      drawNodeContent(node.data, content);
    }).uniformX().growX().row();
    if (node.children.size != 0) {
      table.table(listT -> {
        listT.table(pad -> {
        }).width(15f).uniformY().grow().left();
        listT.table(list -> {
          for (var child : node.children) {
            list.table(Tex.pane, element -> {
              drawNode(child, element);
            }).uniformX().growX().row();
          }
        }).grow().uniformY().right();
      }).uniformX().grow();
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
