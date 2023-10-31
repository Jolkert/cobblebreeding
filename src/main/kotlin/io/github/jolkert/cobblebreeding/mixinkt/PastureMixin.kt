package io.github.jolkert.cobblebreeding.mixinkt

import com.cobblemon.mod.common.block.PastureBlock
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import io.github.jolkert.cobblebreeding.Cobblebreeding
import net.minecraft.block.BlockState
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult

fun overrideUse(hit: BlockHitResult, state: BlockState, pastureBlockEntity: PokemonPastureBlockEntity): CancellableResult<ActionResult>
{
	if (state[PastureBlock.PART] != PastureBlock.PasturePart.BOTTOM || hit.side == state[HorizontalFacingBlock.FACING].opposite)
		return CancellableResult(null, false)

	Cobblebreeding.LOGGER
	return CancellableResult(ActionResult.SUCCESS, true)
}

val HAS_EGG_PROPERTY: BooleanProperty = BooleanProperty.of("has_egg")
