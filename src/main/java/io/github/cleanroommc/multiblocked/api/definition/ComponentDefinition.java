package io.github.cleanroommc.multiblocked.api.definition;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.util.IAxisAlignedBB;
import crafttweaker.mc1120.item.MCItemStack;
import crafttweaker.mc1120.util.MCAxisAlignedBB;
import io.github.cleanroommc.multiblocked.Multiblocked;
import io.github.cleanroommc.multiblocked.api.block.BlockComponent;
import io.github.cleanroommc.multiblocked.api.crafttweaker.functions.*;
import io.github.cleanroommc.multiblocked.api.pattern.BlockInfo;
import io.github.cleanroommc.multiblocked.api.pattern.TraceabilityPredicate;
import io.github.cleanroommc.multiblocked.api.registry.MultiblockComponents;
import io.github.cleanroommc.multiblocked.api.tile.ComponentTileEntity;
import io.github.cleanroommc.multiblocked.client.renderer.IRenderer;
import io.github.cleanroommc.multiblocked.util.RayTraceUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Definition of a component.
 */
@ZenClass("mods.multiblocked.definition.ComponentDefinition")
@ZenRegister
public class ComponentDefinition {
    public final ResourceLocation location;
    public final Class<? extends ComponentTileEntity<?>> clazz;
    public final EnumMap<EnumFacing, List<AxisAlignedBB>> baseAABB;
    public final EnumMap<EnumFacing, List<AxisAlignedBB>> formedAABB;
    @ZenProperty
    public boolean allowRotate;
    @ZenProperty
    public IRenderer baseRenderer;
    @ZenProperty
    public IRenderer formedRenderer;
    @ZenProperty
    public boolean isOpaqueCube;
    @ZenProperty
    public IDrops onDrops;
    @ZenProperty
    public ILeftClick onLeftClick;
    @ZenProperty
    public IRightClick onRightClick;
    @ZenProperty
    public INeighborChanged onNeighborChanged;
    @ZenProperty
    public IGetOutputRedstoneSignal getOutputRedstoneSignal;
    @ZenProperty
    public IUpdateTick updateTick;

    protected ComponentDefinition(ResourceLocation location, Class<? extends ComponentTileEntity<?>> clazz) {
        this.location = location;
        this.clazz = clazz;
        this.baseRenderer = null;
        this.isOpaqueCube = true;
        this.allowRotate = true;
        baseAABB = new EnumMap<>(EnumFacing.class);
        formedAABB = new EnumMap<>(EnumFacing.class);
    }

    public ComponentTileEntity<?> createNewTileEntity(World world){
        try {
            ComponentTileEntity<?> component = clazz.newInstance();
            component.setWorld(world);
            component.setDefinition(this);
            return component;
        } catch (InstantiationException | IllegalAccessException e) {
            Multiblocked.LOGGER.error(e);
        }
        return null;
    }

    public IRenderer getRenderer() {
        return baseRenderer;
    }

    @Override
    @ZenMethod("getLocation")
    @ZenGetter("location")
    public String toString() {
        return location.toString();
    }

    public ItemStack getStackForm() {
        return new ItemStack(MultiblockComponents.COMPONENT_BLOCKS_REGISTRY.get(location), 1);
    }

    @Optional.Method(modid = Multiblocked.MODID_CT)
    @ZenMethod("getStackForm")
    public IItemStack stackForm(){
        return new MCItemStack(getStackForm());
    }

    public void setAABB(boolean isFormed, AxisAlignedBB... aaBBs) {
        if (isFormed) this.formedAABB.clear(); else this.baseAABB.clear();
        EnumMap<EnumFacing, List<AxisAlignedBB>> aabb = isFormed ? this.formedAABB : this.baseAABB;
        Arrays.stream(aaBBs).forEach(aaBB->{
            for (EnumFacing facing : EnumFacing.values()) {
                aabb.computeIfAbsent(facing, f->new ArrayList<>()).add(RayTraceUtils.rotateAABB(aaBB, facing));
            }
        });
    }

    public List<AxisAlignedBB> getAABB(boolean isFormed, EnumFacing facing) {
        return isFormed ? this.formedAABB.getOrDefault(facing, Collections.singletonList(Block.FULL_BLOCK_AABB)) :
                this.baseAABB.getOrDefault(facing, Collections.singletonList(Block.FULL_BLOCK_AABB));
    }

    @Optional.Method(modid = Multiblocked.MODID_CT)
    @ZenGetter("baseAABB")
    public List<IAxisAlignedBB> getBaseAABB() {
        return getAABB(false, EnumFacing.NORTH).stream().map(MCAxisAlignedBB::new).collect(Collectors.toList());
    }

    @Optional.Method(modid = Multiblocked.MODID_CT)
    @ZenSetter("baseAABB")
    public void setBaseAABB(IAxisAlignedBB[] baseAABB) {
        setAABB(false, Arrays.stream(baseAABB).map(CraftTweakerMC::getAxisAlignedBB).toArray(AxisAlignedBB[]::new));
    }

    @Optional.Method(modid = Multiblocked.MODID_CT)
    @ZenGetter("formedAABB")
    public List<IAxisAlignedBB> getFormedAABB() {
        return getAABB(true, EnumFacing.NORTH).stream().map(MCAxisAlignedBB::new).collect(Collectors.toList());
    }

    @Optional.Method(modid = Multiblocked.MODID_CT)
    @ZenSetter("formedAABB")
    public void setFormedAABB(IAxisAlignedBB[] formedAABB) {
        setAABB(true, Arrays.stream(formedAABB).map(CraftTweakerMC::getAxisAlignedBB).toArray(AxisAlignedBB[]::new));
    }

    @ZenMethod
    public TraceabilityPredicate selfPredicate() {
        return this.selfPredicate(false);
    }

    @ZenMethod
    public TraceabilityPredicate selfPredicate(boolean isController) {
        TraceabilityPredicate result = new TraceabilityPredicate(state -> {
            Block block = state.getBlockState().getBlock();
            return block instanceof BlockComponent && ((BlockComponent) block).definition == this;
        }, ()-> new BlockInfo[]{new BlockInfo(MultiblockComponents.COMPONENT_BLOCKS_REGISTRY.get(location))});
        return isController ? result.setCenter() : result;
    }

    public boolean needUpdateTick() {
        return updateTick != null;
    }
    
}
