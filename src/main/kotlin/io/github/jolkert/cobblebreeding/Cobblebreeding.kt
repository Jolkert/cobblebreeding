package io.github.jolkert.cobblebreeding

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object Cobblebreeding : ModInitializer
{
	@JvmStatic val ModId = "cobblebreeding"
	@JvmStatic val DaycareBlock = Block(FabricBlockSettings.create())
	@JvmStatic val Logger = LoggerFactory.getLogger(Cobblebreeding::class.simpleName)!!

	override fun onInitialize()
	{
		registerBlock(Identifier(ModId, "daycare"), DaycareBlock)
		Logger.info("Initialized!")
	}

	private fun registerBlock(identifier: Identifier, block: Block)
	{
		Registry.register(Registries.BLOCK, identifier, block)
		Registry.register(Registries.ITEM, identifier, BlockItem(block, FabricItemSettings()))
	}
}