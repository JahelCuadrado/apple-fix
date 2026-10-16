package com.einsteinmods.applefix.settings;

import com.einsteinmods.applefix.input.InputOptions;
import com.mojang.blaze3d.platform.MacosUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * AppleFix settings built from vanilla widgets and laid out like a YACL screen: a tab bar, the
 * options list on the left two thirds, and on the right a description of the hovered option above
 * the Cancel/Reset, Undo and Save/Done buttons. Mac-only options are hidden on other systems, as
 * vanilla hides its own Mac-only "Ctrl + Click" option.
 *
 * <p>Changes stay pending until "Save Changes", which sets the vanilla {@link OptionInstance}s of
 * {@link InputOptions} and saves {@code options.txt}. "Cancel" (or Escape) discards pending changes
 * and closes; with nothing pending it reads "Reset" and sets every option to its default (still
 * pending, so Undo brings the previous values back).
 */
public final class AppleFixSettingsScreen extends Screen {
  private static final String KEY_PREFIX = "config.applefix.";
  private static final String DESCRIPTION_SUFFIX = ".description";
  private static final String TOOLTIP_SUFFIX = ".tooltip";

  private static final Component OPEN_BUTTON = Component.translatable("options.applefix.settings");
  private static final Component SAVE = Component.translatable(KEY_PREFIX + "save");
  private static final Tooltip SAVE_TOOLTIP = tooltip("save");
  private static final Tooltip DONE_TOOLTIP = tooltip("done");
  private static final Tooltip CANCEL_TOOLTIP = tooltip("cancel");
  private static final Component RESET_ALL = Component.translatable("controls.reset");
  private static final Tooltip RESET_ALL_TOOLTIP = tooltip("reset");
  private static final Component UNDO = Component.translatable(KEY_PREFIX + "undo");
  private static final Tooltip UNDO_TOOLTIP = tooltip("undo");

  /** Darker texture behind the description pane, as YACL. */
  private static final Identifier MENU_LIST_BACKGROUND =
      Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");

  /** Background and separator textures tile in squares of this size. */
  private static final int TEXTURE_SIZE = 32;

  private static final int TAB_HEADER_TEXTURE_SIZE = 16;

  private static final int SEPARATOR_THICKNESS = 2;
  private static final float VERTICAL_RADIANS = (float) Math.toRadians(90.0);

  /** The options list takes two of three columns; the right pane the third. */
  private static final int COLUMNS = 3;

  private static final int LIST_COLUMNS = 2;

  /** Pane padding is this fraction of the column width, as YACL. */
  private static final int PADDING_DIVISOR = 20;

  private static final int MAX_COLUMN_WIDTH = 400;
  private static final int BUTTON_HEIGHT = 20;
  private static final int BUTTON_ROW_GAP = 2;
  private static final int BUTTON_GAP = 4;

  /** A row and the vanilla option it saves to. */
  private record Binding<T>(OptionControl<T> control, OptionInstance<T> option) {
    void save() {
      option.set(control.get());
    }
  }

  private final @Nullable Screen parent;
  private final List<Binding<?>> bindings;
  private final List<OptionControl<?>> controls;

  private final SettingsList list;
  private final OptionDescriptionPanel description;
  private final Button cancelOrReset;
  private final Button undo;
  private final Button saveOrDone;
  private final TabManager tabManager =
      new TabManager(this::addRenderableWidget, this::removeWidget);
  private final ScrollingTab scrollingTab = new ScrollingTab();
  private @Nullable MenuTabBar tabBar;
  private @Nullable ScreenRectangle descriptionArea;
  private @Nullable ScreenRectangle rightPane;

  public AppleFixSettingsScreen(@Nullable Screen parent) {
    super(Component.translatable(KEY_PREFIX + "title"));
    this.parent = parent;
    List<Binding<?>> rows = new ArrayList<>();
    if (MacosUtil.IS_MACOS) {
      rows.add(
          new Binding<>(
              new PercentSliderControl(
                  font,
                  name(InputOptions.TRACKPAD_SPEED_NAME),
                  description(InputOptions.TRACKPAD_SPEED_NAME),
                  InputOptions.MIN_TRACKPAD_SPEED_PERCENT,
                  InputOptions.MAX_TRACKPAD_SPEED_PERCENT,
                  InputOptions.TRACKPAD_HOTBAR_SPEED.get(),
                  InputOptions.DEFAULT_TRACKPAD_SPEED_PERCENT,
                  value -> updateButtons()),
              InputOptions.TRACKPAD_HOTBAR_SPEED));
      rows.add(
          toggle(
              InputOptions.MOMENTUM_SCROLLING,
              InputOptions.MOMENTUM_NAME,
              InputOptions.DEFAULT_MOMENTUM_SCROLLING));
      rows.add(
          toggle(
              InputOptions.HOTBAR_MOMENTUM,
              InputOptions.HOTBAR_MOMENTUM_NAME,
              InputOptions.DEFAULT_HOTBAR_MOMENTUM));
    }
    rows.add(
        toggle(
            InputOptions.REVERSE_SCROLLING,
            InputOptions.REVERSE_SCROLLING_NAME,
            InputOptions.DEFAULT_REVERSE_SCROLLING));
    rows.add(
        toggle(
            InputOptions.REVERSE_HOTBAR_SCROLLING,
            InputOptions.REVERSE_HOTBAR_NAME,
            InputOptions.DEFAULT_REVERSE_HOTBAR_SCROLLING));
    rows.add(
        toggle(
            InputOptions.LOCK_HOTBAR_SCROLLING,
            InputOptions.LOCK_HOTBAR_NAME,
            InputOptions.DEFAULT_LOCK_HOTBAR_SCROLLING));
    bindings = List.copyOf(rows);
    controls = bindings.stream().<OptionControl<?>>map(Binding::control).toList();

    description = new OptionDescriptionPanel(font, controls.getFirst());
    list = new SettingsList(minecraft, description::show);
    controls.forEach(list::addOption);

    cancelOrReset = Button.builder(CommonComponents.GUI_CANCEL, button -> cancelOrReset()).build();
    undo = Button.builder(UNDO, button -> undoAll()).tooltip(UNDO_TOOLTIP).build();
    saveOrDone = Button.builder(CommonComponents.GUI_DONE, button -> saveOrDone()).build();
    updateButtons();
  }

  /** The "AppleFix..." button for vanilla screens; closing the page returns to {@code parent}. */
  public static Button openButton(Screen parent) {
    return Button.builder(
            OPEN_BUTTON,
            button -> Minecraft.getInstance().gui.setScreen(new AppleFixSettingsScreen(parent)))
        .build();
  }

  @Override
  protected void init() {
    MenuTabBar bar = MenuTabBar.builder(tabManager, width).addTabs(scrollingTab).build();
    tabBar = bar;
    addRenderableWidget(bar);
    bar.selectTab(0, false);
    repositionElements();
  }

  @Override
  protected void repositionElements() {
    if (tabBar == null) {
      return;
    }
    tabBar.arrangeElements(width);
    int top = tabBar.getRectangle().bottom();
    tabManager.setTabArea(new ScreenRectangle(0, top, width, height - top));
  }

  @Override
  public void onClose() {
    minecraft.gui.setScreen(parent);
  }

  /** Tab header texture above, menu background below, as vanilla tabbed screens. */
  @Override
  protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
    int headerHeight = tabBar == null ? 0 : tabBar.getRectangle().bottom();
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        CreateWorldScreen.TAB_HEADER_BACKGROUND,
        0,
        0,
        0.0F,
        0.0F,
        width,
        headerHeight,
        TAB_HEADER_TEXTURE_SIZE,
        TAB_HEADER_TEXTURE_SIZE);
    extractMenuBackground(graphics, 0, headerHeight, width, height - headerHeight);
  }

  /** Darker description pane with a separator along its left edge, as YACL. */
  @Override
  public void extractBackground(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractBackground(graphics, mouseX, mouseY, partialTick);
    if (rightPane == null) {
      return;
    }
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        minecraft.level == null
            ? MENU_LIST_BACKGROUND
            : AbstractSelectionList.INWORLD_MENU_LIST_BACKGROUND,
        rightPane.left(),
        rightPane.top(),
        rightPane.right(),
        rightPane.bottom(),
        rightPane.width(),
        rightPane.height(),
        TEXTURE_SIZE,
        TEXTURE_SIZE);
    graphics.pose().pushMatrix();
    graphics.pose().translate(rightPane.left(), rightPane.top());
    graphics.pose().rotate(VERTICAL_RADIANS);
    graphics.blit(
        RenderPipelines.GUI_TEXTURED,
        minecraft.level == null ? FOOTER_SEPARATOR : INWORLD_FOOTER_SEPARATOR,
        0,
        0,
        0.0F,
        0.0F,
        rightPane.height(),
        SEPARATOR_THICKNESS,
        TEXTURE_SIZE,
        SEPARATOR_THICKNESS);
    graphics.pose().popMatrix();
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    if (descriptionArea != null) {
      description.extract(graphics, descriptionArea);
    }
  }

  private Binding<Boolean> toggle(
      OptionInstance<Boolean> option, String optionName, boolean defaultValue) {
    return new Binding<>(
        new TickBoxControl(
            font,
            name(optionName),
            description(optionName),
            option.get(),
            defaultValue,
            value -> updateButtons()),
        option);
  }

  private static Component name(String optionName) {
    return Component.translatable(InputOptions.CAPTION_PREFIX + optionName);
  }

  private static Component description(String optionName) {
    return Component.translatable(InputOptions.CAPTION_PREFIX + optionName + DESCRIPTION_SUFFIX);
  }

  private static Tooltip tooltip(String button) {
    return Tooltip.create(Component.translatable(KEY_PREFIX + button + TOOLTIP_SUFFIX));
  }

  private boolean hasPendingChanges() {
    return controls.stream().anyMatch(OptionControl::isChanged);
  }

  /** Button labels follow YACL: Save Changes / Done, Cancel / Reset; Undo only with changes. */
  private void updateButtons() {
    boolean pending = hasPendingChanges();
    saveOrDone.setMessage(pending ? SAVE : CommonComponents.GUI_DONE);
    saveOrDone.setTooltip(pending ? SAVE_TOOLTIP : DONE_TOOLTIP);
    cancelOrReset.setMessage(pending ? CommonComponents.GUI_CANCEL : RESET_ALL);
    cancelOrReset.setTooltip(pending ? CANCEL_TOOLTIP : RESET_ALL_TOOLTIP);
    undo.active = pending;
  }

  /** Sets the vanilla options and writes {@code options.txt}, as vanilla options screens do. */
  private void saveOrDone() {
    if (!hasPendingChanges()) {
      onClose();
      return;
    }
    bindings.forEach(Binding::save);
    minecraft.options.save();
    controls.forEach(OptionControl::markSaved);
    updateButtons();
  }

  private void cancelOrReset() {
    if (hasPendingChanges()) {
      onClose();
      return;
    }
    controls.forEach(OptionControl::resetQuietly);
    updateButtons();
  }

  private void undoAll() {
    controls.forEach(OptionControl::undo);
    updateButtons();
  }

  /**
   * The single category. Lays out YACL's columns: list on the left two thirds; on the right, the
   * description above Cancel/Reset + Undo, with Save/Done along the bottom.
   */
  private final class ScrollingTab implements Tab {
    private final FrameLayout bounds = new FrameLayout();

    @Override
    public Component getTabTitle() {
      return Component.translatable(KEY_PREFIX + "category.scrolling");
    }

    @Override
    public Component getTabExtraNarration() {
      return Component.empty();
    }

    @Override
    public void visitChildren(Consumer<AbstractWidget> childrenConsumer) {
      childrenConsumer.accept(list);
      childrenConsumer.accept(cancelOrReset);
      childrenConsumer.accept(undo);
      childrenConsumer.accept(saveOrDone);
    }

    @Override
    public void doLayout(ScreenRectangle area) {
      bounds.setPosition(area.left(), area.top());
      bounds.setMinDimensions(area.width(), area.height());
      bounds.arrangeElements();
      int column = area.width() / COLUMNS;
      int padding = column / PADDING_DIVISOR;
      int paneWidth = Math.min(column, MAX_COLUMN_WIDTH) - padding * 2;
      int listWidth = column * LIST_COLUMNS;
      int paneCenter = listWidth + column / 2;
      int paneLeft = paneCenter - paneWidth / 2;

      list.updateSizeAndPosition(listWidth + 1, area.height(), area.left(), area.top());
      rightPane =
          new ScreenRectangle(
              area.left() + listWidth + 1, area.top(), area.width() - listWidth - 1, area.height());

      int saveY = area.bottom() - padding - BUTTON_HEIGHT;
      saveOrDone.setRectangle(paneWidth, BUTTON_HEIGHT, paneLeft, saveY);
      int halfWidth = paneWidth / 2 - BUTTON_GAP / 2;
      int topRowY = saveY - BUTTON_HEIGHT - BUTTON_ROW_GAP;
      cancelOrReset.setRectangle(halfWidth, BUTTON_HEIGHT, paneLeft, topRowY);
      undo.setRectangle(halfWidth, BUTTON_HEIGHT, paneLeft + halfWidth + BUTTON_GAP, topRowY);

      int descriptionTop = area.top() + padding;
      descriptionArea =
          new ScreenRectangle(
              paneLeft, descriptionTop, paneWidth, Math.max(0, topRowY - padding - descriptionTop));
    }

    @Override
    public Layout getLayout() {
      return bounds;
    }
  }
}
