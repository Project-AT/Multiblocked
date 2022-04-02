package com.cleanroommc.multiblocked.common.capability;

import com.cleanroommc.multiblocked.common.capability.widget.AspectStackWidget;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.cleanroommc.multiblocked.api.capability.CapabilityProxy;
import com.cleanroommc.multiblocked.api.capability.IO;
import com.cleanroommc.multiblocked.api.capability.MultiblockCapability;
import com.cleanroommc.multiblocked.api.gui.widget.imp.recipe.ContentWidget;
import com.cleanroommc.multiblocked.api.recipe.Recipe;
import com.cleanroommc.multiblocked.common.recipe.content.AspectStack;
import net.minecraft.tileentity.TileEntity;
import org.apache.commons.lang3.ArrayUtils;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;

public class AspectThaumcraftCapability extends MultiblockCapability<AspectStack> {
    public static final AspectThaumcraftCapability CAP = new AspectThaumcraftCapability();

    private AspectThaumcraftCapability() {
        super("tc6_aspect", new Color(0xCB00C8).getRGB());
    }

    @Override
    public AspectStack defaultContent() {
        return new AspectStack(Aspect.AIR, 1);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return tileEntity instanceof IAspectContainer;
    }

    @Override
    public AspectThaumcraftCapabilityProxy createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return new AspectThaumcraftCapabilityProxy(tileEntity);
    }

    @Override
    public AspectStack copyInner(AspectStack content) {
        return content.copy();
    }

    @Override
    public ContentWidget<AspectStack> createContentWidget() {
        return new AspectStackWidget();
    }

    @Override
    public AspectStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new AspectStack(Aspect.getAspect(jsonElement.getAsJsonObject().get("aspect").getAsString()), jsonElement.getAsJsonObject().get("amount").getAsInt());
    }

    @Override
    public JsonElement serialize(AspectStack aspectStack, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("aspect", aspectStack.aspect.getTag());
        jsonObj.addProperty("amount", aspectStack.amount);
        return jsonObj;
    }

    public static class AspectThaumcraftCapabilityProxy extends CapabilityProxy<AspectStack> {

        public AspectThaumcraftCapabilityProxy(TileEntity tileEntity) {
            super(AspectThaumcraftCapability.CAP, tileEntity);
        }

        public IAspectContainer getCapability() {
            return (IAspectContainer)getTileEntity();
        }

        @Override
        protected List<AspectStack> handleRecipeInner(IO io, Recipe recipe, List<AspectStack> left, boolean simulate) {
            IAspectContainer capability = getCapability();
            if (capability == null) return left;
            Iterator<AspectStack> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    AspectStack aspectStack = iterator.next();
                    Aspect aspect = aspectStack.aspect;
                    int amount = aspectStack.amount;
                    if (!ArrayUtils
                            .contains(capability.getAspects().getAspects(), aspect)) return left;
                    int stored = capability.getAspects().getAmount(aspect);
                    aspectStack.amount = Math.max(0, stored - amount);
                    if (!simulate) {
                        capability.takeFromContainer(aspect, stored - aspectStack.amount);
                    }
                    if (aspectStack.amount <= 0) {
                        iterator.remove();
                    }
                }
            } else if (io == IO.OUT){
                while (iterator.hasNext()) {
                    AspectStack aspectStack = iterator.next();
                    Aspect aspect = aspectStack.aspect;
                    int amount = aspectStack.amount;
                    int ll = capability.addToContainer(aspect, amount);
                    aspectStack.amount = ll;
                    if (simulate && amount - ll > 0) {
                        capability.takeFromContainer(aspect, amount - ll);
                    }
                    if (aspectStack.amount <= 0) {
                        iterator.remove();
                    }
                }
            }
            return left.isEmpty() ? null : left;
        }

    }
}