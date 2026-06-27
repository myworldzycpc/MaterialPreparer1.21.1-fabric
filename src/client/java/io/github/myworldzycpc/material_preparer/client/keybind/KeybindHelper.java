package io.github.myworldzycpc.material_preparer.client.keybind;

import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;

public class KeybindHelper {
    /**
     * 用于 EventHandler - 创建带 mc.screen 防护的 Runnable
     */
    public static Runnable withScreenGuard(Runnable action) {
        return () -> {
            if (mc.gui.screen() != null) return;
            action.run();
        };
    }

    /**
     * 用于 ModMenuIntegration - 创建 ActionKeybind 选项（一行搞定）
     */
    public static Option<Component> actionOption(String name, String desc, KeybindEntry entry, boolean closeGui) {
        return Option.<Component>createBuilder()
                .name(Component.translatable(name))
                .description(OptionDescription.of(Component.translatable(desc)))
                .binding(entry.keybind.getDisplayName(), entry.keybind::getDisplayName, val -> {
                })
                .customController(opt -> new ActionKeybindController(
                        opt, entry.keybind,
                        () -> {
                            if (closeGui && mc.gui.screen() != null) mc.gui.screen().onClose();
                            entry.action.run();
                        },
                        kb -> {
                            entry.configSaver.accept(kb.serialize());
                            MaterialPreparerConfig.HANDLER.save();
                        }
                ))
                .build();
    }

    /**
     * 用于 ModMenuIntegration - 创建 TickBoxKeybind 选项
     */
    public static Option<Boolean> tickBoxOption(String name, String desc, KeybindEntry entry, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable(name))
                .description(OptionDescription.of(Component.translatable(desc)))
                .binding(false, getter, setter)
                .customController(opt -> new TickBoxKeybindController(
                        opt, entry.keybind,
                        kb -> {
                            entry.configSaver.accept(kb.serialize());
                            MaterialPreparerConfig.HANDLER.save();
                        }
                ))
                .build();
    }
}