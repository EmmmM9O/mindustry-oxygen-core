package com.github.emmmm9o.oxygencore.ui;

import com.github.emmmm9o.oxygencore.blocks.OxygenMessageBlock;
import com.github.emmmm9o.oxygencore.blocks.OxygenMessageBlock.OxygenMessageBuild;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.TextArea;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.gen.Icon;

public class MessageBlockWindow extends BlockWindow {
  protected OxygenMessageBuild messageBuild;

  public MessageBlockWindow(OxygenMessageBuild building) {
    super(building);
    this.messageBuild = building;
  }

  @Override
  public void drawControlTable(Table controlTable) {
    controlTable.table(cont -> {
      textArea = cont.add(new TextArea(messageBuild.message.toString().replace("\r", "\n"))).uniform().fill().grow().left()
          .get();
      textArea.setFilter((textField, c) -> {
        if (c == '\n') {
          int count = 0;
          for (int i = 0; i < textField.getText().length(); i++) {
            if (textField.getText().charAt(i) == '\n') {
              count++;
            }
          }
          return count < ((OxygenMessageBlock) building.block()).maxNewlines;
        }
        return true;
      });
      textArea.setMaxLength(((OxygenMessageBlock) building.block()).maxTextLength);
      cont.label(() -> textArea.getText()).uniform().fill().grow().right();
    }).grow().uniformX().minHeight(240f).minWidth(100f).get().setBackground(StyleManager.style.bodyBackground);
  }

  @Override
  public void drawStatus(Table tab) {
    tab.label(() -> textArea.getText().length() + " / " + ((OxygenMessageBlock) building.block()).maxTextLength)
        .color(Color.lightGray).grow().left().height(48).get().setAlignment(Align.center);
    ;
    tab.table(buttons -> {
      buttons.button(Icon.save, StyleManager.style.windowButtons, () -> {
        save();
      }).size(48);
      buttons.button(Icon.trash, StyleManager.style.windowButtons, () -> {
        textArea.setText("");
        save();
      }).size(48);
      buttons.button(Icon.copy, StyleManager.style.windowButtons, () -> {
        Core.app.setClipboardText(textArea.getText().replace('\r', '\n'));
      }).size(48);
      buttons.button(Icon.paste, StyleManager.style.windowButtons, () -> {
        textArea.setText(Core.app.getClipboardText().replace('\r', '\n'));
        save();
      }).size(48);
    }).right().height(48);
  }

  public TextArea textArea;

  public void save() {
    if (!textArea.getText().equals(messageBuild.message.toString()))
      messageBuild.configure(textArea.getText());
  }

  @Override
  public void onClose() {
    super.onClose();
    save();
  }

}
