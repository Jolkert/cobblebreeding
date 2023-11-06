package io.github.jolkert.cobblebreeding.mixinkt

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.Move
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.feature.FlagSpeciesFeature
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.block.PastureBlock
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.IVs
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
import kotlin.random.Random

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

	val baby = PokemonProperties()
	// species
	val species = baseSpecies(dominantParent.species).let {
		if (it.resourceIdentifier.toString().startsWith("cobblemon:nidoran"))
			PokemonSpecies.getByIdentifier(cobblemonResource(if (Random.nextBoolean()) "nidoranm" else "nidoranf"))!!
		else if (it.resourceIdentifier == cobblemonResource("volbeat") || it.resourceIdentifier == cobblemonResource("illumise"))
			PokemonSpecies.getByIdentifier(cobblemonResource(if (Random.nextBoolean()) "volbeat" else "illumise"))!!
		else
			it
	}
	baby.species = species.showdownId()

	// form
	/*
		this form code is terrible. really it *should* just take the dominant parents form as well but the games are weird
		like in game, breeding two johto woopers in paldea give you a paldean wooper. ????
		just seems a bit silly to me. since theres no "region" it doesnt really make sense for it to work that way in cobblemon anyways?
		idk maybe i'll change. i dont *like* breaking ingame consistency, but is it really consistent as is? i dont think so
		- morgan 2023-11-05
	*/
	val everstoneForms = parents.mapToList {
		if (it.heldItem().isIn(CobblemonItemTags.EVERSTONE)
			&& it.isSameEvoLineAs(species))
		{
			it.form.formOnlyShowdownId()
		}
		else
			null
	}.filterNotNull()
	val nonEverstoneForms = parents.mapToList {
		if (it.isSameEvoLineAs(species))
			it.form.formOnlyShowdownId()
		else
			null
	}.filterNotNull()

	val form = everstoneForms.ifEmpty { nonEverstoneForms }.random().let { form ->
		species.forms.firstOrNull { it.formOnlyShowdownId() == form } ?: species.standardForm
	}
	baby.form = form.name
	form.aspects.forEach { baby.customProperties.add(FlagSpeciesFeature(it, true)) }

	// nature
	val heritableNatures = parents.mapToList {
		if (it.heldItem().isIn(CobblemonItemTags.EVERSTONE))
			it.nature
		else
			null
	}.filterNotNull()
	baby.nature = (if (heritableNatures.isEmpty()) Natures.getRandomNature() else heritableNatures.random()).name.path

	// ball
	baby.pokeball = (if (!parents.areSameSpecies()) dominantParent else parents.random()).caughtBall.let {
		if (!it.stack().isIn(Cobblebreeding.UNINHERITABLE_BALLS))
			it
		else
			PokeBalls.POKE_BALL
	}.name.path

	// IVs
	val statsPriority = CobblemonStatProvider.ofType(Stat.Type.PERMANENT).shuffled().toMutableList()
	val powerItems = parents.mapToList {
		if (it.heldItem().isIn(Cobblebreeding.POWER_ITEMS))
			it.heldItem().associatedStat()!! to it
		else
			null
	}.filterNotNull()

	baby.ivs = IVs.createRandomIVs()
	val parentHasPowerItem = powerItems.isNotEmpty()
	if (parentHasPowerItem)
	{
		val (stat, parent) = powerItems.random()
		baby.ivs!![stat] = parent.ivs[stat]!!
		statsPriority.remove(stat)
	}
	val numStatsToInherit = (if (parents.either { it.heldItem().isIn(CobblemonItemTags.DESTINY_KNOT) }) 5 else 3) -
			(if (parentHasPowerItem) 1 else 0)

	for (stat in statsPriority.take(numStatsToInherit))
		baby.ivs!![stat] = parents.random().ivs[stat]!!

	// Moves
	val moveset = mutableSetOf<MoveTemplate>()
	moveset.addAll(species.moves.getLevelUpMovesUpTo(1))
	moveset.addAll((species.moves.getLevelUpMovesUpTo(100) + species.moves.eggMoves).filter { move ->
		parents.either { parent -> parent.moveSet.map { it.template.name }.contains(move.name) }
	})

	return baby.create().apply {
		for ((i, move) in moveset.withIndex())
		{
			if (i < 4)
				this.moveSet.add(move.create())
			else
				this.benchedMoves.add(BenchedMove(move, 0))
		}
	}
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

/*
	do different stages of the same line count as the same species?
	illumise + volbeat? nidof + nidom??
	bulba is nonspecific. im unsure
	- morgan 2023-11-04
*/
private fun Pair<Pokemon, Pokemon>.areSameSpecies() =
	first.species.resourceIdentifier == second.species.resourceIdentifier

private fun Pokemon.isDitto() = this.species.resourceIdentifier == cobblemonResource("ditto")
private tailrec fun baseSpecies(species: Species): Species
{
	// for some reason kotlin doesnt like tailrec on extension functions?
	// i feel like extensions should just be smoothed into static methods at compilation but ig not ¯\_(ツ)_/¯ -morgan 2023-11-04
	val preEvo = species.preEvolution?.species ?: return species
	return baseSpecies(preEvo)
}

private fun Pokemon.isSameEvoLineAs(other: Species) =
	baseSpecies(this.species).resourceIdentifier == baseSpecies(other).resourceIdentifier

@JvmField
val HAS_EGG_PROPERTY: BooleanProperty = BooleanProperty.of("has_egg")
