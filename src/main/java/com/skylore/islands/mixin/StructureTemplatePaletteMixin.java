package com.skylore.islands.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla Palette.cache is a HashMap. VS/DH chunk workers call blocks(Block) on the same
 * shared template and crash with ConcurrentModificationException.
 */
@Mixin(StructureTemplate.Palette.class)
public abstract class StructureTemplatePaletteMixin {

    @Shadow
    @Final
    @Mutable
    private Map<Block, List<StructureTemplate.StructureBlockInfo>> cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void skylore$threadSafeCache(List<StructureTemplate.StructureBlockInfo> blocks, CallbackInfo ci) {
        this.cache = new ConcurrentHashMap<>();
    }
}
