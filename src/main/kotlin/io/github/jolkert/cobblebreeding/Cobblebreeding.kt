package io.github.jolkert.cobblebreeding

import net.fabricmc.api.ModInitializer
import net.minecraft.item.Item
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object Cobblebreeding : ModInitializer
{
	const val MOD_ID = "cobblebreeding"

	@JvmField
	val LOGGER = LoggerFactory.getLogger(Cobblebreeding::class.simpleName)!!

	@JvmField
	val POWER_ITEMS = TagKey.of(RegistryKeys.ITEM, Identifier(MOD_ID, "held/power_item"))

	@JvmField
	val UNINHERITABLE_BALLS: TagKey<Item> = TagKey.of(RegistryKeys.ITEM, Identifier(MOD_ID, "balls/uninheritable_balls"))

	override fun onInitialize()
	{
		LOGGER.info("Initialized!")
	}
}
