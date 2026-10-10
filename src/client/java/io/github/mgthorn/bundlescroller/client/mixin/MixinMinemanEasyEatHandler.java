package io.github.mgthorn.bundlescroller.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import io.github.mgthorn.bundlescroller.client.compat.MinemanEasyEat;
import io.github.mgthorn.bundlescroller.client.config.Configs;

/**
 * Auto eat from bundle: when Garitone Mineman's easy eat finds no food in the inventory,
 * food is taken out of a bundle into a free slot and its index is returned instead of -1.
 * {@code @Pseudo}: the mixin is skipped when Garitone Mineman isn't installed.
 */
@Pseudo
@Mixin(targets = "io.github.mgthorn.garitonemineman.client.feature.EasyEatHandler", remap = false)
public abstract class MixinMinemanEasyEatHandler {
	// Mineman's own food check, so its autoEatBlacklist applies to bundle contents too
	@Shadow
	private static boolean isFood(ItemStack stack) {
		throw new AssertionError();
	}

	// require = 0: a changed Mineman version only disables this feature instead of crashing the game
	@Inject(method = "findBestFoodIndex", at = @At("RETURN"), cancellable = true, require = 0)
	private static void bundleScroller$takeFoodFromBundle(Minecraft mc, int targetHotbarSlot, boolean includeTargetSlot,
														  CallbackInfoReturnable<Integer> cir) {
		if (cir.getReturnValueI() != -1 || !Configs.Generic.AUTO_EAT_FROM_BUNDLE.getBooleanValue()) {
			return;
		}

		int index = MinemanEasyEat.takeFoodFromBundle(mc, targetHotbarSlot, stack -> isFood(stack));

		if (index != -1) {
			cir.setReturnValue(index);
		}
	}
}
