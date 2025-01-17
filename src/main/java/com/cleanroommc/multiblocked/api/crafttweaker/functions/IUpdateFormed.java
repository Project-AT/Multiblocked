package com.cleanroommc.multiblocked.api.crafttweaker.functions;

import com.cleanroommc.multiblocked.api.crafttweaker.interfaces.ICTController;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

@FunctionalInterface
@ZenClass("mods.multiblocked.functions.IUpdateFormed")
@ZenRegister
public interface IUpdateFormed {
    void apply(ICTController controllerTileEntity);
}
