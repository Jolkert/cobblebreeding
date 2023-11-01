package io.github.jolkert.cobblebreeding.mixinkt

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.block.PastureBlock
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.stat.CobblemonStatProvider
import com.cobblemon.mod.common.util.cobblemonResource
import io.github.jolkert.cobblebreeding.Cobblebreeding
import io.github.jolkert.cobblebreeding.util.*
import net.minecraft.block.BlockState
import net.minecraft.block.HorizontalFacingBlock
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult

fun overrideUse(
	hit: BlockHitResult,
	state: BlockState,
	pastureBlockEntity: PokemonPastureBlockEntity,
	player: PlayerEntity
): CancellableResult<ActionResult>
{
	if (state[PastureBlock.PART] != PastureBlock.PasturePart.BOTTOM || hit.side == state[HorizontalFacingBlock.FACING].opposite)
		return CancellableResult(null, false)

	val breedingPairs = pastureBlockEntity
		.tetheredPokemon
		.mapNotNull { it.getPokemon() }
		.combinationsWithSelf()
		.filter { it.canBreed() }

	if (breedingPairs.isNotEmpty())
	{
		val baby = breed(breedingPairs.random())
		Cobblemon.storage.getParty(player.uuid).add(baby)
	}

	return CancellableResult(ActionResult.SUCCESS, true)
}


private fun breed(parents: Pair<Pokemon, Pokemon>): Pokemon
{
	val dominantParent = (if (parents.either { it.isDitto() })
		parents.firstMatching { !it.isDitto() }
	else
		parents.firstMatching { it.gender == Gender.FEMALE })
		?: parents.first // one of the parents should either be female or non-ditto or something has gone wrong, but we march on either way -morgan 2023-10-31

	val baby = Pokemon()

	// species
	baby.species = dominantParent.species.baseForm()
	// todo: special cases for nidos, volbeat/illumise, and manaphy[??]
	// todo: regional forms
	// todo: fucking incense?

	// nature
	val heritableNatures = parents.mapToList {
		if (it.heldItem().isIn(CobblemonItemTags.EVERSTONE))
			it.nature
		else
			null
	}.filterNotNull()

	baby.nature = if (heritableNatures.isEmpty()) Natures.getRandomNature() else heritableNatures.random()

	// ball
	baby.caughtBall = (if (!parents.areSameSpecies()) dominantParent else parents.random()).caughtBall.let {
		if (!it.stack().isIn(Cobblebreeding.UNINHERITABLE_BALLS))
			it
		else
			PokeBalls.POKE_BALL
	}

	// IVs
	val statsPriority = CobblemonStatProvider.ofType(Stat.Type.PERMANENT).shuffled().toMutableList()

	val powerItems = parents.mapToList {
		if (it.heldItem().isIn(Cobblebreeding.POWER_ITEMS))
			it.heldItem().associatedStat()!! to it
		else
			null
	}.filterNotNull()

	val parentHasPowerItem = powerItems.isNotEmpty()
	if (parentHasPowerItem)
	{
		val (stat, parent) = powerItems.random()
		baby.ivs[stat] = parent.ivs[stat]!!
		statsPriority.remove(stat)
	}
	val numStatsToInherit = (if (parents.either { it.heldItem().isIn(CobblemonItemTags.DESTINY_KNOT) }) 5 else 3) -
			(if (parentHasPowerItem) 1 else 0)

	for (stat in statsPriority.take(numStatsToInherit))
		baby.ivs[stat] = parents.random().ivs[stat]!!

	return baby
}

private fun ItemStack.associatedStat(): Stat? =
	if (this.isIn(CobblemonItemTags.POWER_ANKLET))
		Stats.SPEED
	else if (this.isIn(CobblemonItemTags.POWER_BAND))
		Stats.SPECIAL_DEFENCE
	else if (this.isIn(CobblemonItemTags.POWER_BELT))
		Stats.DEFENCE
	else if (this.isIn(CobblemonItemTags.POWER_BRACER))
		Stats.ATTACK
	else if (this.isIn(CobblemonItemTags.POWER_LENS))
		Stats.SPECIAL_ATTACK
	else if (this.isIn(CobblemonItemTags.POWER_WEIGHT))
		Stats.HP
	else
		null

private fun Pair<Pokemon, Pokemon>.canBreed(): Boolean
{
	if (!(onlyOne { it.isDitto() } || (onlyOne { it.gender == Gender.MALE } && onlyOne { it.gender == Gender.FEMALE })))
		return false

	if (either { it.species.eggGroups.contains(EggGroup.UNDISCOVERED) })
		return false

	if (onlyOne { it.isDitto() })
		return true

	return first.species.eggGroups.containsAny(second.species.eggGroups)
}
private fun Pair<Pokemon, Pokemon>.areSameSpecies() = first.species.resourceIdentifier == second.species.resourceIdentifier

private fun Pokemon.isDitto() = this.species.resourceIdentifier == cobblemonResource("ditto")
private fun Species.baseForm(): Species
{
	val preEvo = this.preEvolution
	return preEvo?.species?.baseForm() ?: this
}

private inline fun <T, R> Pair<T, T>.mapToList(transform: (T) -> R) = listOf(transform(first), transform(second))

@JvmField
val HAS_EGG_PROPERTY: BooleanProperty = BooleanProperty.of("has_egg")
