package io.github.jolkert.cobblebreeding.mixin;

import com.cobblemon.mod.common.block.PastureBlock;
import io.github.jolkert.cobblebreeding.Cobblebreeding;
import io.github.jolkert.cobblebreeding.mixinkt.CancellableResult;
import io.github.jolkert.cobblebreeding.mixinkt.PastureMixinKt;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(PastureBlock.class)
public class PastureBlockMixin
{
	@Inject(method = "onUse",
			at = @At(value = "FIELD",
					target = "Lcom/cobblemon/mod/common/Cobblemon;INSTANCE:Lcom/cobblemon/mod/common/Cobblemon;",
					opcode = Opcodes.GETSTATIC),
			cancellable = true,
			locals = LocalCapture.CAPTURE_FAILHARD)
	void test(BlockState state,
			  World world,
			  BlockPos pos,
			  PlayerEntity player,
			  Hand hand,
			  BlockHitResult hit,
			  CallbackInfoReturnable<ActionResult> cir,
			  BlockPos basePos,
			  BlockEntity baseEntity)
	{
		CancellableResult<ActionResult> result = PastureMixinKt.overrideUse(hit, state, basePos);
		if (result.getValue() != null)
			cir.setReturnValue(result.getValue());
		if (result.getShouldCancel())
			cir.cancel();
	}
}
