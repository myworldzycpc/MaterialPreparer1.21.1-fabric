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
import net.minecraft.world.inventory.ShulkerBoxMenu;
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
        if (currentExplorationPhase == ExplorationPhase.WAIT_OUTPUT_OPEN) currentExplorationPhase = ExplorationPhase.WAIT_OUTPUT_SET_CONTENTS;
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
        if (mc.player.containerMenu instanceof ChestMenu || mc.player.containerMenu instanceof ShulkerBoxMenu) {
            if (lastInteractionRecord.type == InteractionRecord.Type.CHEST) {
                BlockPos pos = lastInteractionRecord.pos;
                if (mc.level == null) return;
                BlockState state = mc.level.getBlockState(pos);
                List<ItemStack> chestItems = null;
                if (mc.player.containerMenu instanceof ChestMenu chestMenu) {
                    chestItems = ((SimpleContainer) chestMenu.getContainer()).getItems();
                } else if (mc.player.containerMenu instanceof ShulkerBoxMenu shulkerBoxMenu) {
                    chestItems = shulkerBoxMenu.getItems().subList(0, 27);
                }
                if (state.getBlock() instanceof ChestBlock) {
                    ChestType chestType = state.getValue(ChestBlock.TYPE);
                    if (chestType == ChestType.LEFT) {
                        pos = getOppositeChestPos(pos, state.getValue(ChestBlock.FACING), chestType);
                    }
                    MaterialPreparerClient.updateChestItems(pos, chestItems);
                } else if (isChestLikeContainerBlock(state.getBlock())) {
                    MaterialPreparerClient.updateChestItems(pos, chestItems);
                }
            } else {
                MaterialPreparerClient.showMessage(Component.translatable("message.material_preparer.chest_record_not_chest"));
            }
        }

        // 转移物品到输出容器
        if (currentExplorationPhase == ExplorationPhase.WAIT_OUTPUT_SET_CONTENTS) {
            if (mc.player.containerMenu instanceof ChestMenu || mc.player.containerMenu instanceof ShulkerBoxMenu) {
                int playerInvStart = getPlayerInventoryStartSlot();
                // 遍历玩家背包，将物品列表中的物品转移到输出容器
                for (int i = 0; i < 36; i++) {
                    int slot = playerInvStart + i;
                    ItemStack stack = mc.player.containerMenu.getSlot(slot).getItem();
                    if (!stack.isEmpty()) {
                        // 检查是否在物品列表中
                        boolean inItemList = false;
                        for (var entry : itemList) {
                            if (entry.item() == stack.getItem()) {
                                inItemList = true;
                                break;
                            }
                        }
                        if (inItemList) {
                            quickMoveContainerSlot(slot);
                        }
                    }
                }
                currentExplorationPhase = ExplorationPhase.TRANSFERRING_TO_OUTPUT;
            } else {
                // 不是箱子，跳过，直接关闭
                currentExplorationPhase = ExplorationPhase.WAIT_OUTPUT_CLOSE;
            }
        }

        if (currentExplorationPhase == ExplorationPhase.WAIT_SET_CONTENTS) {
            if (isCollectingItems) {
                currentExplorationPhase = ExplorationPhase.PROCESS_NEXT_ITEM;
            } else {
                currentExplorationPhase = ExplorationPhase.WAIT_CLOSE;
            }
        }
    }
}
