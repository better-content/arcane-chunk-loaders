package com.bettercontent.arcanechunkloaders.item;

import com.bettercontent.arcanechunkloaders.AnchorVariant;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class AnchorBlockItem extends BlockItem {
    private final AnchorVariant variant;

    public AnchorBlockItem(Block block, AnchorVariant variant, Properties properties) {
        super(block, properties);
        this.variant = variant;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Keeps a centered 3x3 chunk area fully ticking").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal("Redstone disables loading but not charging").withStyle(ChatFormatting.GRAY));
        CompoundTag blockTag = stack.getTagElement("BlockEntityTag");
        if (blockTag == null) {
            tooltip.add(Component.literal("Stored charge: empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.literal("Stored charge: " + storedText(blockTag)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Charge and inventory are retained when placed").withStyle(ChatFormatting.GRAY));
    }

    private String storedText(CompoundTag tag) {
        return switch (variant) {
            case FLUX -> tag.getInt("fe") + " FE";
            case KINETIC -> tag.getInt("charge") + " service ticks";
            case SOURCE -> tag.getInt("source") + " Source";
            case LIFEFORCE -> tag.getCompound("lifeforce").getInt("Amount") + " mB life essence";
            case PRESSURE -> tag.getInt("air") + " air";
            case SOUL -> tag.getInt("soul") + " soul energy";
            case AUREAL -> tag.getInt("aureal") + " Aureal";
            case SPIRIT -> spiritCount(tag.getCompound("spirits")) + " spirits";
        };
    }

    private static int spiritCount(CompoundTag handler) {
        if (!handler.contains("Items", Tag.TAG_LIST)) return 0;
        ListTag items = handler.getList("Items", Tag.TAG_COMPOUND);
        return items.isEmpty() ? 0 : ((CompoundTag) items.get(0)).getByte("Count") & 0xff;
    }
}
