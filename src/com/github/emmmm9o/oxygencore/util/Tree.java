package com.github.emmmm9o.oxygencore.util;

import arc.func.Func;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Nullable;

public class Tree<E> {
  public TreeNode<E> root;

  public Tree(TreeNode<E> root) {
    this.root = root;
  }

  public static class TreeNode<E> {

    public E data;
    public @Nullable TreeNode<E> father;
    public Seq<TreeNode<E>> children;
    public int depth = 0;

    public boolean isRoot() {
      return father == null;
    }

    public TreeNode(E data, TreeNode<E> father, Seq<TreeNode<E>> children, int depth) {
      this.data = data;
      this.father = father;
      this.children = children;
      this.depth = depth;
    }
  }

  public static class FParsrr<E> {
    public Seq<Tree<E>> trees;
    public ObjectMap<E, TreeNode<E>> nodes;// TMp
    public Seq<E> datas;
    public Func<E, E> getFather;

    public FParsrr(Seq<E> datas, Func<E, E> getFather) {
      trees = new Seq<Tree<E>>();
      nodes = new ObjectMap<>();
      this.getFather = getFather;
      this.datas = datas;
    }

    public FParsrr(Seq<Tree<E>> trees) {
      this.trees = trees;
    }

    public TreeNode<E> newNode(E data) {
      if (nodes.containsKey(data)) {
        return nodes.get(data);
      } else {
        var node =  new TreeNode<E>(data, null, new Seq<>(), -1);
	nodes.put(data,node);
        var fa = getFather.get(data);
        if (fa == null) {
          node.depth = 0;
        } else {
          node.father = newNode(fa);
          node.father.children.add(node);
          node.depth = node.father.depth + 1;
        }
        return node;
      }

    }

    public Seq<Tree<E>> parse() {
      for (var data : datas) {
        newNode(data);
      }
      for (var node : nodes.values()) {
        if (node.isRoot()) {
          trees.add(new Tree<E>(node));
        }
      }
      return trees;
    }
  }
}
