package io.github.mgthorn.bundlescroller.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.player.LocalPlayer;
import io.github.mgthorn.bundlescroller.client.pick.BundlePicker;

/**
 * Tells auto restock when the hand was emptied with the drop key, so {@code autoRestockAfterDrop} can skip it.
 */
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {
	@Inject(method = "drop", at = @At("HEAD"))
	private void bundleScroller$onDrop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
		BundlePicker.onDrop();
	}
}
