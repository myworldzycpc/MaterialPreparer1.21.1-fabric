package io.github.myworldzycpc.material_preparer.client;

import net.minecraft.core.BlockPos;

public class InteractionRecord {
    public enum Type {
        CHEST,
        CRAFTING_TABLE,
        OTHER
    }

    public BlockPos pos;
    public Type type;
    public Long timestamp;

    public InteractionRecord() {
        this.pos = null;
        this.type = null;
        this.timestamp = null;
    }

    public void updateRecord(BlockPos pos, Type type) {
        this.pos = pos;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public void clearRecord() {
        this.pos = null;
        this.type = null;
        this.timestamp = null;
    }

    public boolean isRecordValid() {
        return this.pos != null && this.type != null && this.timestamp != null;
    }
}
