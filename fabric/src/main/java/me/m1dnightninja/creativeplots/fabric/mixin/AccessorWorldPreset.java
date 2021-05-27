package me.m1dnightninja.creativeplots.fabric.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(WorldPreset.class)
public interface AccessorWorldPreset {

    @Accessor("PRESETS")
    static List<WorldPreset> getPresets() {
        throw new AssertionError();
    }

}
