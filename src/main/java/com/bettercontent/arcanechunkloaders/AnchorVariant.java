package com.bettercontent.arcanechunkloaders;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

public enum AnchorVariant {
    FLUX("flux_chunk_anchor", "Flux Chunk Anchor", "minecraft:redstone_block", "Buffers Forge Energy (FE/RF) from any side"),
    KINETIC("kinetic_chunk_anchor", "Kinetic Chunk Anchor", "minecraft:raw_gold_block", "Buffers Create rotational work from its vertical shaft"),
    SOURCE("source_chunk_anchor", "Source Chunk Anchor", "ars_nouveau:source_gem_block", "Accepts Source through the Ars Nouveau source interface"),
    LIFEFORCE("lifeforce_chunk_anchor", "Lifeforce Chunk Anchor", "bloodmagic:bloodstonebrick", "Accepts Blood Magic life essence fluid from any side"),
    PRESSURE("pressure_chunk_anchor", "Pressure Chunk Anchor", "pneumaticcraft:compressed_iron_block", "Accepts PneumaticCraft air through pressure tubes"),
    SOUL("soul_chunk_anchor", "Soul Chunk Anchor", "goety:ominous_stone", "Sneak-use with an empty hand to transfer Goety soul energy"),
    SPIRIT("spirit_chunk_anchor", "Spirit Chunk Anchor", "malum:block_of_soulstone", "Use Malum spirits on the anchor or insert them automatically");

    private final String id;
    private final String displayName;
    private final ResourceLocation coreBlock;
    private final String inputDescription;

    AnchorVariant(String id, String displayName, String coreBlock, String inputDescription) {
        this.id = id;
        this.displayName = displayName;
        this.coreBlock = new ResourceLocation(coreBlock);
        this.inputDescription = inputDescription;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String inputDescription() { return inputDescription; }

    public Block coreBlock() {
        Block block = ForgeRegistries.BLOCKS.getValue(coreBlock);
        return block == null || block == Blocks.AIR ? Blocks.AMETHYST_BLOCK : block;
    }
}
