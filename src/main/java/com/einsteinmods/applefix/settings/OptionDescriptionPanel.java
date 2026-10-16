package com.einsteinmods.applefix.settings;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * Right-hand pane text, as YACL's option description: the option name in bold, then its wrapped
 * description, clipped to the pane. Shows the last option hovered or focused.
 */
final class OptionDescriptionPanel {
  /** Gap between the name and the description, as YACL. */
  private static final int PARAGRAPH_GAP = 5;

  private final Font font;
  private OptionControl<?> shown;

  /** Wrapped lines of the texts below at {@link #wrapWidth}; rebuilt only when one changes. */
  private List<FormattedCharSequence> nameLines = List.of();

  private List<FormattedCharSequence> descriptionLines = List.of();
  private @Nullable Component wrappedName;
  private @Nullable Component wrappedDescription;
  private int wrapWidth;

  OptionDescriptionPanel(Font font, OptionControl<?> initial) {
    this.font = font;
    this.shown = initial;
  }

  void show(OptionControl<?> option) {
    shown = option;
  }

  void extract(GuiGraphicsExtractor graphics, ScreenRectangle area) {
    rewrapIfNeeded(area.width());
    graphics.enableScissor(area.left(), area.top(), area.right(), area.bottom());
    int y = lines(graphics, nameLines, area.left(), area.top()) + PARAGRAPH_GAP;
    lines(graphics, descriptionLines, area.left(), y);
    graphics.disableScissor();
  }

  private int lines(
      GuiGraphicsExtractor graphics, List<FormattedCharSequence> lines, int x, int top) {
    int y = top;
    for (FormattedCharSequence line : lines) {
      graphics.text(font, line, x, y, OptionControl.TEXT_COLOR, true);
      y += font.lineHeight;
    }
    return y;
  }

  /** Wraps again only when the option, its text (a cycled value) or the width changed. */
  private void rewrapIfNeeded(int width) {
    Component name = shown.name();
    Component description = shown.description();
    if (name == wrappedName && description == wrappedDescription && width == wrapWidth) {
      return;
    }
    wrappedName = name;
    wrappedDescription = description;
    wrapWidth = width;
    nameLines = font.split(name.copy().withStyle(ChatFormatting.BOLD), width);
    descriptionLines = font.split(description, width);
  }
}
