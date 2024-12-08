package cn.ussshenzhou.madparticle.designer.gui.widegt;

import cn.ussshenzhou.madparticle.command.MadParticleCommand;
import cn.ussshenzhou.madparticle.designer.gui.DesignerScreen;
import cn.ussshenzhou.madparticle.designer.gui.panel.HelperModePanel;
import cn.ussshenzhou.madparticle.designer.gui.panel.ParametersScrollPanel;
import cn.ussshenzhou.t88.gui.advanced.TConstrainedEditBox;
import cn.ussshenzhou.t88.gui.combine.TTitledSelectList;
import cn.ussshenzhou.t88.gui.util.LayoutHelper;
import cn.ussshenzhou.t88.gui.util.MouseHelper;
import cn.ussshenzhou.t88.gui.widegt.TButton;
import cn.ussshenzhou.t88.gui.widegt.TSelectList;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author USS_Shenzhou
 */
public class CommandStringSelectList extends TTitledSelectList<CommandStringSelectList.SubCommand> {
    public final TButton newCommand = new TButton(Component.translatable("gui.mp.de.helper.new"));
    public final TButton delete = new TButton(Component.translatable("gui.mp.de.helper.delete"));

    public CommandStringSelectList() {
        super(Component.translatable("gui.mp.de.helper.command_chain"), new TSelectList<>());
        this.add(newCommand);
        this.add(delete);
        initButton();
        delete.setOnPress(pButton -> {
            getComponent().removeElement(getComponent().getSelected());
            delete.getParentInstanceOf(HelperModePanel.class).setParametersScrollPanel(null);
            this.checkChild();
        });
    }

    protected void initButton() {
        newCommand.setOnPress(pButton -> {
            var list = getComponent();
            var sub = new CommandStringSelectList.SubCommand();
            add(sub.parametersScrollPanel);
            addElement(sub, list1 -> {
                list1.getParentInstanceOf(HelperModePanel.class).setParametersScrollPanel(list1.getSelected().getContent().parametersScrollPanel);
            });
            if (list.getSelected() == null) {
                list.setSelected(list.children().get(list.children().size() - 1));
            }
            this.checkChild();
        });
        delete.setOnPress(pButton -> {
            getComponent().removeElement(getComponent().getSelected());
            delete.getParentInstanceOf(HelperModePanel.class).setParametersScrollPanel(null);
            this.checkChild();
        });
    }

    public void checkChild() {
        var list = this.getComponent().children();
        for (int i = 0; i < list.size(); i++) {
            var panel = list.get(i).getContent().parametersScrollPanel;
            if (i == 0) {
                if (panel.isChild()) {
                    panel.setChild(false);
                }
            } else {
                if (!panel.isChild()) {
                    panel.setChild(true);
                }
            }
        }
    }

    @Override
    public void layout() {
        LayoutHelper.BBottomOfA(newCommand, DesignerScreen.GAP * 2 + 1, DesignerScreen.getInstance().getDesignerModeSelectList(),
                TButton.RECOMMEND_SIZE.x, TButton.RECOMMEND_SIZE.y);
        LayoutHelper.BBottomOfA(delete, DesignerScreen.GAP * 2 + 1,
                this, TButton.RECOMMEND_SIZE.x, TButton.RECOMMEND_SIZE.y);
        super.layout();
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
        //This is a bad example. You should use panels instead of direct fill() to draw split lines.
        graphics.fill(x + width,
                y,
                x + width + 1,
                delete.y + delete.getHeight(),
                0x80ffffff
        );
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (isInRange(MouseHelper.getMouseX(), MouseHelper.getMouseY(), 4, 4)) {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
        return false;
    }

    public SubCommand addSubCommand(ParametersScrollPanel parametersScrollPanel) {
        return new SubCommand(parametersScrollPanel);
    }

    public class SubCommand {
        public final ParametersScrollPanel parametersScrollPanel;

        public SubCommand() {
            this(new ParametersScrollPanel());
        }

        public SubCommand(ParametersScrollPanel parametersScrollPanel) {
            this.parametersScrollPanel = parametersScrollPanel;
            add(parametersScrollPanel);
        }

        @Override
        public String toString() {
            String value = parametersScrollPanel.target.getComponent().getEditBox().getValue();
            if (value.isEmpty()) {
                return "null";
            }
            String[] s = value.split(":");
            return s[s.length - 1];
        }

        public ParametersScrollPanel getParametersScrollPanel() {
            return parametersScrollPanel;
        }
    }

    public String warp() {
        StringBuilder builder = new StringBuilder("mp");
        Iterator<TSelectList<SubCommand>.Entry> iterator = this.getComponent().children().iterator();
        while (iterator.hasNext()) {
            var subCommand = iterator.next();
            String sub = subCommand.getContent().parametersScrollPanel.wrap();
            Thread.startVirtualThread(() -> checkWrapped(subCommand, sub));
            builder.append(sub);
            if (iterator.hasNext()) {
                builder.append(" expireThen");
            }
        }
        return builder.toString();
    }

    private void checkWrapped(TSelectList<?>.Entry entry, String subCommand) {
        subCommand = "mp" + subCommand;
        subCommand = subCommand.replace("=", "0");
        ParseResults<CommandSourceStack> parseResults = MadParticleCommand.justParse(subCommand);
        Map<?, CommandSyntaxException> map = parseResults.getExceptions();
        if ((!map.isEmpty()) || parseResults.getContext().build(subCommand).getNodes().isEmpty()) {
            entry.setSpecialForeground(TConstrainedEditBox.RED_TEXT_COLOR);
        } else {
            entry.clearSpecialForeground();
        }
    }
}
