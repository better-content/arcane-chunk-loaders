package com.gerald.arcanechunkloaders.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnchorSavedDataSchemaTest {
    @Test
    void build40EnvelopeRoundTripsAndStampsSchema() {
        CompoundTag written = new AnchorSavedData().save(new CompoundTag());
        assertEquals(1, written.getInt("schema"));
        assertEquals(0, AnchorSavedData.load(written).sortedRecords().size());
    }

    @Test
    void unknownNewerSchemaFailsBeforeAnyRewrite() {
        CompoundTag newer = new CompoundTag();
        newer.putInt("schema", 2);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> AnchorSavedData.load(newer));
        assertEquals("Unsupported Arcane Chunkloaders save schema 2; expected 1. Refusing to rewrite world data.", error.getMessage());
    }

    @Test
    void preBuild40EnvelopeIsNotAccepted() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> AnchorSavedData.load(new CompoundTag()));
        assertEquals("Unsupported Arcane Chunkloaders save schema 0; expected 1. Refusing to rewrite world data.", error.getMessage());
    }
}
