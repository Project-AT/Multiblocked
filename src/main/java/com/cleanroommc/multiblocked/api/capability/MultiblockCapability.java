package com.cleanroommc.multiblocked.api.capability;

import com.cleanroommc.multiblocked.Multiblocked;
import com.cleanroommc.multiblocked.api.block.BlockComponent;
import com.cleanroommc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.cleanroommc.multiblocked.api.capability.trait.CapabilityTrait;
import com.cleanroommc.multiblocked.api.gui.texture.ColorRectTexture;
import com.cleanroommc.multiblocked.api.gui.widget.WidgetGroup;
import com.cleanroommc.multiblocked.api.gui.widget.imp.LabelWidget;
import com.cleanroommc.multiblocked.api.gui.widget.imp.recipe.ContentWidget;
import com.cleanroommc.multiblocked.api.pattern.util.BlockInfo;
import com.cleanroommc.multiblocked.api.registry.MbdComponents;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import crafttweaker.annotations.ZenRegister;
import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenProperty;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Used to detect whether a machine has a certain capability. And provide its capability in proxy {@link CapabilityProxy}.
 *
 */
@ZenClass("mods.multiblocked.capability.Capability")
@ZenRegister
public abstract class MultiblockCapability<T> implements JsonSerializer<T>, JsonDeserializer<T> {
    @ZenProperty
    public final String name;
    @ZenProperty
    public final int color;

    protected MultiblockCapability(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public String getUnlocalizedName() {
        return "multiblocked.capability." + name;
    }

    /**
     * default content for the RecipeMapWidget selector
     */
    public abstract T defaultContent();

    /**
     * detect whether this block has capability
     */
    public abstract boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity);

    /**
     * deep copy of this content. recipe need it for searching and such things
     */
    public abstract T copyInner(T content);
    
    /**
     * create a proxy of this block.
     */
    public abstract CapabilityProxy<? extends T> createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity);

    /**
     * Create a Widget of given contents
     */
    public ContentWidget<? super T> createContentWidget() {
        return new ContentWidget<T>() {
            @Override
            protected void onContentUpdate() {
                if (Multiblocked.isClient()) {
                    setHoverTooltip(I18n.format("multiblocked.content.miss", io, I18n.format(MultiblockCapability.this.getUnlocalizedName()), content.toString()));
                }
            }

            @Override
            public void openConfigurator(WidgetGroup dialog) {
                super.openConfigurator(dialog);
                dialog.addWidget(new LabelWidget(5, 30, "multiblocked.gui.label.configurator"));
            }

        }.setBackground(new ColorRectTexture(color));
    }

    public boolean hasTrait() {
        return false;
    }

    public CapabilityTrait createTrait() {
        return null;
    }

    public <C> Set<C> getCapability(Capability<C> capability, @Nonnull TileEntity tileEntity) {
        Set<C> found = new LinkedHashSet<>();
        for (EnumFacing facing : EnumFacing.VALUES) {
            C cap = tileEntity.getCapability(capability, facing);
            if (cap != null) return Collections.singleton(cap);
        }
        return found;
    }

    /**
     * Get candidate blocks for display in JEI as well as automated builds
     */
    public abstract BlockInfo[] getCandidates();

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public final BlockComponent getAnyBlock() {
        return MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, name + ".any"));
    }

    public final JsonElement serialize(Object obj) {
        return serialize((T)obj, null, null);
    }

    public final T deserialize(JsonElement jsonElement){
        return deserialize(jsonElement, null, null);
    }

}
