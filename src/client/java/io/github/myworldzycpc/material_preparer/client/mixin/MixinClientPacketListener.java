package io.github.myworldzycpc.material_preparer.client.mixin;

import io.github.myworldzycpc.material_preparer.client.ExplorationPhase;
import io.github.myworldzycpc.material_preparer.client.InteractionRecord;
import io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.*;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleOpenScreen", at = @At("RETURN"), cancellable = false)
    public void handleOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (mc.player == null) return;
//        MaterialPreparerClient.showMessage(Component.literal("Container Opened Packet Received: " + packet));
        if (currentExplorationPhase == ExplorationPhase.WAIT_OPEN) currentExplorationPhase = ExplorationPhase.WAIT_SET_CONTENTS;
    }

    @Inject(method = "handleContainerSetSlot", at = @At("RETURN"), cancellable = false)
    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (mc.player == null) return;
//        MaterialPreparerClient.showMessage(Component.literal("Container Set Slot Packet Received: " + packet));
    }

    @Inject(method = "handleContainerContent", at = @At("RETURN"), cancellable = false)
    public void handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (mc.player == null) return;
//        MaterialPreparerClient.showMessage(Component.literal("Container Content Packet Received: " + packet));
        if (mc.player.containerMenu instanceof ChestMenu chestMenu) {
            if (lastInteractionRecord.type == InteractionRecord.Type.CHEST) {
                BlockPos pos = lastInteractionRecord.pos;
                if (mc.level == null) return;
                BlockState state = mc.level.getBlockState(pos);
                if (state.getBlock() instanceof ChestBlock) {
                    ChestType chestType = state.getValue(ChestBlock.TYPE);
                    if (chestType == ChestType.LEFT) {
                        pos = getOppositeChestPos(pos, state.getValue(ChestBlock.FACING), chestType);
                    }
                    MaterialPreparerClient.updateChestItems(pos, ((SimpleContainer) chestMenu.getContainer()).getItems());
                }
            } else {
                MaterialPreparerClient.showMessage(Component.literal("Chest Interaction Record Type is not Chest, ignoring packet."));
            }
        }
        if (isCollectingItems && mc.player.containerMenu instanceof ChestMenu chestMenu) {
            List<ItemStack> chestItems = ((SimpleContainer) chestMenu.getContainer()).getItems();
            for (int i = 0; i < chestItems.size(); i++) {
                ItemStack stack = chestItems.get(i);
                if (!stack.isEmpty() && neededItems.containsKey(stack.getItem())) {
                    int remaining = neededItems.get(stack.getItem());
                    int moving = Math.min(remaining, stack.getCount());
                    if (moving > 0) {
                        neededItems.put(stack.getItem(), remaining - moving);
                        quickMoveContainerSlot(i);
                    }
                }
            }
        }
        if (currentExplorationPhase == ExplorationPhase.WAIT_SET_CONTENTS) currentExplorationPhase = ExplorationPhase.WAIT_CLOSE;
    }
}
