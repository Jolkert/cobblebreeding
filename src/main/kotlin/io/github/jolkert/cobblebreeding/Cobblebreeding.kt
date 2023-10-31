package io.github.jolkert.cobblebreeding

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Cobblebreeding : ModInitializer
{
	// val MOD_ID = "cobblebreeding"
	val LOGGER = LoggerFactory.getLogger(Cobblebreeding::class.simpleName)!!

	override fun onInitialize()
	{
		LOGGER.info("Initialized!")
	}
}
