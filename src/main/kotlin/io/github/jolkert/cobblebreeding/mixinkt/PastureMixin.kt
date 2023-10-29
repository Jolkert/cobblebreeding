package io.github.jolkert.cobblebreeding.mixinkt

import com.cobblemon.mod.common.block.PastureBlock
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import io.github.jolkert.cobblebreeding.Cobblebreeding
import net.minecraft.block.BlockState
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos

fun overrideUse(hit: BlockHitResult, state: BlockState, basePos: BlockPos): CancellableResult<ActionResult>
{
	Cobblebreeding.Logger.warn("basePos = $basePos")
	return if (state[PastureBlock.PART] == PastureBlock.PasturePart.BOTTOM && hit.side == state[HorizontalFacingBlock.FACING])
		CancellableResult(ActionResult.SUCCESS, true)
	else
		CancellableResult(null, false)
}