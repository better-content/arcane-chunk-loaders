package com.gerald.arcanechunkloaders.blockentity;

import com.gerald.arcanechunkloaders.AnchorConfig;
import com.gerald.arcanechunkloaders.AnchorMath;
import com.gerald.arcanechunkloaders.AnchorRegistries;
import com.gerald.arcanechunkloaders.AnchorVariant;
import com.gerald.arcanechunkloaders.ArcaneChunkLoadersMod;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.UUID;

public final class ArcaneAnchorBlockEntity extends BlockEntity implements AnchorAccess, ISourceTile, IAirHandler {
    public static final TagKey<net.minecraft.world.item.Item> MALUM_SPIRITS = TagKey.create(
            Registries.ITEM, new ResourceLocation(ArcaneChunkLoadersMod.MOD_ID, "malum_spirits"));
    private final AnchorData anchor = new AnchorData();
    private final AnchorVariant variant;
    private int fe;
    private int source;
    private int air;
    private int soul;
    private int aureal;
    private final FluidTank lifeforce = new FluidTank(AnchorConfig.LIFEFORCE_CAPACITY.get(), this::isLifeEssence) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final ItemStackHandler spirits = new ItemStackHandler(1) {
        @Override public boolean isItemValid(int slot, @Nonnull ItemStack stack) { return stack.is(MALUM_SPIRITS); }
        @Override public int getSlotLimit(int slot) { return AnchorConfig.SPIRIT_CAPACITY.get(); }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(EnergyBuffer::new);
    private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> fluidCap = LazyOptional.of(() -> lifeforce);
    private LazyOptional<net.minecraftforge.items.IItemHandler> itemCap = LazyOptional.of(() -> spirits);
    private LazyOptional<IAirHandler> airCap = LazyOptional.of(() -> this);

    public ArcaneAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(AnchorRegistries.ARCANE_ANCHOR.get(), pos, state);
        this.variant = ((com.gerald.arcanechunkloaders.block.ArcaneAnchorBlock) state.getBlock()).variant();
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ArcaneAnchorBlockEntity blockEntity) {
        blockEntity.anchor.tick(blockEntity);
    }

    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ArcaneAnchorBlockEntity blockEntity) {
        if (state.getValue(com.gerald.arcanechunkloaders.block.ArcaneAnchorBlock.ACTIVE) && level.getGameTime() % 20 == 0 && level.random.nextInt(3) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    (level.random.nextDouble() - 0.5) * 0.08, 0.03, (level.random.nextDouble() - 0.5) * 0.08);
        }
    }

    public void placedBy(Player player) {
        anchor.placedBy(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public void removed() {
        if (level instanceof ServerLevel) anchor.onRemoved(this);
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = player.getItemInHand(hand);
        if (variant == AnchorVariant.SPIRIT && !held.isEmpty() && held.is(MALUM_SPIRITS)) {
            ItemStack remainder = spirits.insertItem(0, held.copy(), false);
            int moved = held.getCount() - remainder.getCount();
            if (moved > 0 && !player.getAbilities().instabuild) held.shrink(moved);
            player.displayClientMessage(Component.literal("Inserted " + moved + " Malum spirit" + (moved == 1 ? "" : "s") + "."), true);
            setChangedAndSync();
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty() && variant == AnchorVariant.SOUL) {
            return transferPlayerPower(player, true) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && held.isEmpty() && variant == AnchorVariant.AUREAL) {
            return transferPlayerPower(player, false) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        if (held.isEmpty()) {
            player.displayClientMessage(Component.literal(variant.displayName() + ": " + chargeText() +
                    (level.hasNeighborSignal(worldPosition) ? " (redstone disabled)" : anchor.ticketsActive() ? " (active)" : " (starved)")), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private boolean transferPlayerPower(Player player, boolean goety) {
        int room = goety ? AnchorConfig.SOUL_CAPACITY.get() - soul : AnchorConfig.AUREAL_CAPACITY.get() - aureal;
        if (room <= 0) {
            player.displayClientMessage(Component.literal("The anchor is already full."), true);
            return true;
        }
        try {
            if (goety) {
                Class<?> helper = Class.forName("com.Polarice3.Goety.utils.SEHelper");
                Method get = helper.getMethod("getSESouls", Player.class);
                Method decrease = helper.getMethod("decreaseSESouls", Player.class, int.class);
                int moved = Math.min(room, (Integer) get.invoke(null, player));
                if (moved <= 0 || !((Boolean) decrease.invoke(null, player, moved))) return false;
                soul += moved;
                invokeOptional(helper, "sendSEUpdatePacket", player);
                player.displayClientMessage(Component.literal("Transferred " + moved + " soul energy."), true);
            } else {
                Class<?> helper = Class.forName("com.stal111.forbidden_arcanus.common.aureal.AurealHelper");
                Object capability = helper.getMethod("getCapability", Player.class).invoke(null, player);
                Method get = capability.getClass().getMethod("getAureal");
                Method decrease = capability.getClass().getMethod("decreaseAureal", int.class);
                int moved = Math.min(room, (Integer) get.invoke(capability));
                if (moved <= 0 || !((Boolean) decrease.invoke(capability, moved))) return false;
                aureal += moved;
                invokeOptional(helper, "sendAurealUpdatePacket", player);
                player.displayClientMessage(Component.literal("Transferred " + moved + " Aureal."), true);
            }
            setChangedAndSync();
            return true;
        } catch (ReflectiveOperationException error) {
            ArcaneChunkLoadersMod.LOGGER.warn("Could not transfer {} power", goety ? "Goety soul" : "Aureal", error);
            return false;
        }
    }

    private static void invokeOptional(Class<?> owner, String name, Player player) {
        try { owner.getMethod(name, Player.class).invoke(null, player); } catch (ReflectiveOperationException ignored) {}
    }

    @Override public ServerLevel serverLevel() { return (ServerLevel) level; }
    @Override public BlockPos anchorPos() { return worldPosition; }
    @Override public AnchorVariant variant() { return variant; }
    @Override public UUID anchorId() { return anchor.id(); }
    @Override public void passiveCharge() {}

    @Override
    public boolean consumePower(long gameTime) {
        boolean consumed = switch (variant) {
            case FLUX -> consumeFe();
            case SOURCE -> consumeSource();
            case LIFEFORCE -> consumeLifeforce();
            case PRESSURE -> consumeAir();
            case SOUL -> consumeTimedSoul(gameTime);
            case SPIRIT -> consumeTimedSpirit(gameTime);
            case AUREAL -> consumeTimedAureal(gameTime);
            case KINETIC -> false;
        };
        if (consumed) setChanged();
        return consumed;
    }

    private boolean consumeFe() {
        int cost = AnchorConfig.FE_PER_TICK.get();
        if (fe < cost) return false;
        fe -= cost;
        return true;
    }

    private boolean consumeSource() {
        int cost = AnchorConfig.SOURCE_PER_TICK.get();
        if (source < cost) return false;
        source -= cost;
        return true;
    }

    private boolean consumeLifeforce() {
        int cost = AnchorConfig.LIFEFORCE_PER_TICK.get();
        if (lifeforce.getFluidAmount() < cost) return false;
        lifeforce.drain(cost, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private boolean consumeAir() {
        int cost = AnchorConfig.AIR_PER_TICK.get();
        if (air < cost) return false;
        air -= cost;
        return true;
    }

    private boolean consumeTimedSoul(long gameTime) {
        if (soul <= 0) return false;
        if (gameTime % AnchorConfig.SOUL_INTERVAL.get() == 0) soul--;
        return true;
    }

    private boolean consumeTimedSpirit(long gameTime) {
        ItemStack stack = spirits.getStackInSlot(0);
        if (stack.isEmpty()) return false;
        if (gameTime % AnchorConfig.SPIRIT_INTERVAL.get() == 0) spirits.extractItem(0, 1, false);
        return true;
    }

    private boolean consumeTimedAureal(long gameTime) {
        if (aureal <= 0) return false;
        if (gameTime % AnchorConfig.AUREAL_INTERVAL.get() == 0) aureal--;
        return true;
    }

    @Override
    public double chargeFraction() {
        return switch (variant) {
            case FLUX -> ratio(fe, AnchorConfig.FE_CAPACITY.get());
            case SOURCE -> ratio(source, AnchorConfig.SOURCE_CAPACITY.get());
            case LIFEFORCE -> ratio(lifeforce.getFluidAmount(), AnchorConfig.LIFEFORCE_CAPACITY.get());
            case PRESSURE -> ratio(air, AnchorConfig.AIR_CAPACITY.get());
            case SOUL -> ratio(soul, AnchorConfig.SOUL_CAPACITY.get());
            case SPIRIT -> ratio(spirits.getStackInSlot(0).getCount(), AnchorConfig.SPIRIT_CAPACITY.get());
            case AUREAL -> ratio(aureal, AnchorConfig.AUREAL_CAPACITY.get());
            case KINETIC -> 0.0;
        };
    }

    private static double ratio(int value, int max) { return AnchorMath.chargeFraction(value, max); }

    @Override
    public String chargeText() {
        return switch (variant) {
            case FLUX -> fe + " / " + AnchorConfig.FE_CAPACITY.get() + " FE";
            case SOURCE -> source + " / " + AnchorConfig.SOURCE_CAPACITY.get() + " Source";
            case LIFEFORCE -> lifeforce.getFluidAmount() + " / " + AnchorConfig.LIFEFORCE_CAPACITY.get() + " mB life essence";
            case PRESSURE -> air + " / " + AnchorConfig.AIR_CAPACITY.get() + " air";
            case SOUL -> soul + " / " + AnchorConfig.SOUL_CAPACITY.get() + " soul energy";
            case SPIRIT -> spirits.getStackInSlot(0).getCount() + " / " + AnchorConfig.SPIRIT_CAPACITY.get() + " spirits";
            case AUREAL -> aureal + " / " + AnchorConfig.AUREAL_CAPACITY.get() + " Aureal";
            case KINETIC -> "not kinetic";
        };
    }

    @Override public void setVisualActive(boolean active) {
        if (level != null && getBlockState().hasProperty(com.gerald.arcanechunkloaders.block.ArcaneAnchorBlock.ACTIVE)
                && getBlockState().getValue(com.gerald.arcanechunkloaders.block.ArcaneAnchorBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(com.gerald.arcanechunkloaders.block.ArcaneAnchorBlock.ACTIVE, active), 3);
        }
    }
    @Override public void markAnchorChanged() { setChangedAndSync(); }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private boolean isLifeEssence(FluidStack stack) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return id != null && id.toString().equals("bloodmagic:life_essence_fluid");
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        anchor.save(tag);
        tag.putInt("fe", fe);
        tag.putInt("source", source);
        tag.putInt("air", air);
        tag.putInt("soul", soul);
        tag.putInt("aureal", aureal);
        tag.put("lifeforce", lifeforce.writeToNBT(new CompoundTag()));
        tag.put("spirits", spirits.serializeNBT());
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        anchor.load(tag);
        fe = tag.getInt("fe");
        source = tag.getInt("source");
        air = tag.getInt("air");
        soul = tag.getInt("soul");
        aureal = tag.getInt("aureal");
        if (tag.contains("lifeforce")) lifeforce.readFromNBT(tag.getCompound("lifeforce"));
        if (tag.contains("spirits")) spirits.deserializeNBT(tag.getCompound("spirits"));
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) { if (packet.getTag() != null) load(packet.getTag()); }

    @Override public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate(); fluidCap.invalidate(); itemCap.invalidate(); airCap.invalidate();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        energyCap = LazyOptional.of(EnergyBuffer::new);
        fluidCap = LazyOptional.of(() -> lifeforce);
        itemCap = LazyOptional.of(() -> spirits);
        airCap = LazyOptional.of(() -> this);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (variant == AnchorVariant.FLUX && cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (variant == AnchorVariant.LIFEFORCE && cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
        if (variant == AnchorVariant.SPIRIT && cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
        if (variant == AnchorVariant.PRESSURE && cap == PNCCapabilities.AIR_HANDLER_CAPABILITY) return airCap.cast();
        return super.getCapability(cap, side);
    }

    @Override public int getTransferRate() { return 1_000; }
    @Override public boolean canAcceptSource() { return variant == AnchorVariant.SOURCE && source < AnchorConfig.SOURCE_CAPACITY.get(); }
    @Override public int getSource() { return variant == AnchorVariant.SOURCE ? source : 0; }
    @Override public int getMaxSource() { return variant == AnchorVariant.SOURCE ? AnchorConfig.SOURCE_CAPACITY.get() : 0; }
    @Override public void setMaxSource(int max) {}
    @Override public int setSource(int amount) { source = Math.max(0, Math.min(AnchorConfig.SOURCE_CAPACITY.get(), amount)); setChanged(); return source; }
    @Override public int addSource(int amount) { int accepted = Math.min(Math.max(0, amount), AnchorConfig.SOURCE_CAPACITY.get() - source); source += accepted; setChanged(); return source; }
    @Override public int removeSource(int amount) { int removed = Math.min(Math.max(0, amount), source); source -= removed; setChanged(); return source; }

    @Override public float getPressure() { return getBaseVolume() <= 0 ? 0.0f : air / (float) getBaseVolume(); }
    @Override public int getAir() { return air; }
    @Override public void addAir(int amount) {
        air = (int) Math.max(0L, Math.min((long) AnchorConfig.AIR_CAPACITY.get(), (long) air + amount));
        setChanged();
    }
    @Override public int getBaseVolume() { return Math.max(1, AnchorConfig.AIR_CAPACITY.get() / 20); }
    @Override public void setBaseVolume(int volume) {}
    @Override public int getVolume() { return getBaseVolume(); }
    @Override public float maxPressure() { return 20.0f; }

    private final class EnergyBuffer implements IEnergyStorage {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.min(Math.max(0, maxReceive), AnchorConfig.FE_CAPACITY.get() - fe);
            if (!simulate && accepted > 0) { fe += accepted; setChanged(); }
            return accepted;
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return fe; }
        @Override public int getMaxEnergyStored() { return AnchorConfig.FE_CAPACITY.get(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    }
}
