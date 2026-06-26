package com.example.dimensionbleed;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DimensionBleedMod implements ModInitializer {
    public static final String MOD_ID = "dimension_bleed";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Dimension Bleed initialized: chunk generation positions are remapped from a unique per-chunk seed.");
    }
}
