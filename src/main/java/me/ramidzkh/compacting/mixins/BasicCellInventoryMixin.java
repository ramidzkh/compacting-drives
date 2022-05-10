package me.ramidzkh.compacting.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.me.cells.BasicCellInventory;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import me.ramidzkh.compacting.CompactingDrives;
import me.ramidzkh.compacting.CompactionHandler;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class BasicCellInventoryMixin {

    @Shadow
    public abstract IUpgradeInventory getUpgradesInventory();

    @Shadow
    protected abstract Object2LongMap<AEKey> getCellItems();

    @Nullable
    private CompactionHandler handler;

    private boolean recursionBreak;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInitialize(CallbackInfo callbackInfo) {
        if (getUpgradesInventory().isInstalled(CompactingDrives.COMPACTING_CARD)) {
            handler = new CompactionHandler((BasicCellInventory) (Object) this, this::getCellItems);
        }
    }

    @Inject(method = "getAvailableStacks", at = @At("HEAD"), cancellable = true)
    private void getAvailableStacks(KeyCounter out, CallbackInfo callbackInfo) {
        if (handler != null) {
            handler.getAvailableStacks(out);
            callbackInfo.cancel();
        }
    }

    @Inject(method = "extract", at = @At("RETURN"), cancellable = true)
    private void onExtract(AEKey what, long amount, Actionable mode, IActionSource source,
            CallbackInfoReturnable<Long> callbackInfoReturnable) {
        if (recursionBreak) {
            return;
        }

        var extracted = callbackInfoReturnable.getReturnValueJ();

        if (extracted != amount && handler != null) {
            recursionBreak = true;

            // If not full extraction, attempt compaction extraction
            if (mode == Actionable.MODULATE) {
                callbackInfoReturnable
                        .setReturnValue(
                                extracted + handler.extract(what, amount - extracted, Actionable.MODULATE, source));
            } else {
                callbackInfoReturnable.setReturnValue(handler.extract(what, amount, Actionable.SIMULATE, source));
            }

            recursionBreak = false;
        }
    }
}
