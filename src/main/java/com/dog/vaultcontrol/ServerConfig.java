package com.dog.vaultcontrol;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {

    public static final ForgeConfigSpec CONFIG;
    public static final Config CONFIG_VALUES;

    static {
        final Pair<Config, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Config::new);
        CONFIG = specPair.getRight();
        CONFIG_VALUES = specPair.getLeft();
    }

    public static class Config {
        public final ForgeConfigSpec.BooleanValue AutoSaveLogs;
        public final ForgeConfigSpec.BooleanValue AutoSaves;
        public final ForgeConfigSpec.BooleanValue TPSListener;
        public final ForgeConfigSpec.BooleanValue MobAIControl;
        public final ForgeConfigSpec.BooleanValue VaultRaidEffect;
        public final ForgeConfigSpec.BooleanValue SafeSaving;

        Config(ForgeConfigSpec.Builder builder) {
            builder.push("Features");

            AutoSaves = builder.comment(" Asynchronously save the world every 5 minutes").define("AutoSaves", true);
            SafeSaving = builder.comment(" Prevents players from entering portals while saves occur").define("SafeSaving", false);
            AutoSaveLogs = builder.comment(" Enable in-game logs for auto saves").define("AutoSaveLogs", false);
            TPSListener = builder.comment(" Listen for TPS and disable chunk loaders if it drops too low").define("TPSListener", false);
            MobAIControl = builder.comment(" Mob AI will be controlled to reduce entity lag").define("MobAIControl", false);
            VaultRaidEffect = builder.comment(" Remove raid effects from users upon leaving the vault").define("VaultRaidEffect", false);

            builder.pop();
        }
    }

}
