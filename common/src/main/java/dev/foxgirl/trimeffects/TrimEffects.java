package dev.foxgirl.trimeffects;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class TrimEffects {

    public static final Logger LOGGER = LogManager.getLogger("trimeffects");

    private static TrimEffects INSTANCE;

    public static @NotNull TrimEffects getInstance() {
        return INSTANCE;
    }

    public static @NotNull TrimEffects createInstance() {
        return new TrimEffects();
    }

    private TrimEffects() {
        INSTANCE = this;
    }

    private Config.Parsed config;

    public @NotNull Config.Parsed getConfig() {
        return Objects.requireNonNull(config, "Expression 'config'");
    }

    public void initialize(@NotNull Path configDirectory) {
        config = Config.read(configDirectory).parse();
    }

    public static @NotNull DynamicRegistryManager getRegistryManager(@NotNull Entity entity) {
        return entity.getWorld().getRegistryManager();
    }

    public static <T> @NotNull RegistryKey<T> getKey(@NotNull RegistryEntry<T> entry) {
        return entry.getKey().orElseThrow();
    }

    public static @Nullable ArmorTrim getTrim(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
        return stack.get(DataComponentTypes.TRIM);
    }

    private record Trim(@NotNull ArmorTrim trim) {
        private static Trim from(@NotNull DynamicRegistryManager manager, @NotNull ItemStack stack) {
            var trim = getTrim(manager, stack);
            return trim == null ? null : new Trim(trim);
        }

        private @NotNull RegistryEntry<ArmorTrimMaterial> getMaterial() {
            return trim.getMaterial();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            var that = (Trim) obj;
            return Objects.equals(this.getMaterial(), that.getMaterial());
        }
    }

    public void handleTick(LivingEntity player) {
        var manager = getRegistryManager(player);

        var armor = (List<ItemStack>) player.getArmorItems();
        var trims = new Trim[armor.size()];

        for (int i = 0, length = trims.length; i < length; i++) {
            trims[i] = Trim.from(manager, armor.get(i));
        }

        boolean fullSet = Arrays.stream(trims).allMatch(Objects::nonNull); // Check if all armor pieces have trims
        if (fullSet) {
            for (Trim trim : trims) {
                if (trim == null) continue;
                int count = (int) Arrays.stream(trims).filter(t -> Objects.equals(t, trim)).count();
                if (count >= getConfig().getMinimumMatchingTrims()) {
                    handleTickForTrim(player, trim);
                }
            }
        }
    }

    private void handleTickForTrim(LivingEntity player, Trim trim) {
        var manager = getRegistryManager(player);
        var material = getKey(trim.getMaterial());

        switch (material.getValue().toString()) {
            case "minecraft:diamond":
                applyDiamondEffect(player);
                break;
            case "minecraft:netherite":
                applyNetheriteEffect(player);
                break;
            case "minecraft:redstone":
                applyRedstoneEffect(player);
                break;
            case "minecraft:quartz":
                applyQuartzEffect(player);
                break;
            case "minecraft:emerald":
                applyEmeraldEffect(player);
                break;
            case "minecraft:lapis":
                applyLapisEffect(player);
                break;
            case "minecraft:copper":
                applyCopperEffect(player);
                break;
            case "minecraft:iron":
                applyIronEffect(player);
                break;
            case "minecraft:gold":
                applyGoldEffect(player);
                break;
            case "minecraft:amethyst": // Amethyst Trim Effect
                applyAmethystEffect(player);
                break;
            default:
                break;
        }
    }

    // Method to check if near a beacon with Haste II
    private static boolean isNearHaste2Beacon(LivingEntity player) {
        BlockPos pos = player.getBlockPos();
        int radius = 50;  // Set range for detecting the beacon
        for (BlockPos beaconPos : BlockPos.iterateOutwards(pos, radius, radius, radius)) {
            BlockState state = player.getWorld().getBlockState(beaconPos);
            if (state.isOf(Blocks.BEACON)) {
                // Assuming the beacon has Haste II for simplicity
                LOGGER.info("Beacon with Haste II detected nearby");
                return true;
            }
        }
        LOGGER.info("No Haste II Beacon found nearby");
        return false;
    }

    // Check if the block is a Deepslate variant or Deepslate Ore
    private static boolean isDeepslateVariant(BlockState blockState) {
        return blockState.isOf(Blocks.DEEPSLATE)
            || blockState.isOf(Blocks.DEEPSLATE_COAL_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_COPPER_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_EMERALD_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_GOLD_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_IRON_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_LAPIS_ORE)
            || blockState.isOf(Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    // Method to check if the player is in a cave (below y-level 45 or in low light)
    private static boolean isInCave(LivingEntity player) {
        return player.getBlockY() < 45 || player.getWorld().getLightLevel(player.getBlockPos()) < 7;  // Low light level indicating a cave
    }


    // Diamond Trim Effects
    private static void applyDiamondEffect(LivingEntity player) {
        // Always apply Haste II
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 1, false, true));  // Haste II

        // Apply Night Vision when the player is below y-level 45 or in a cave
        if (player.getBlockY() < 45 || isInCave(player)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, true));  // Night Vision
            LOGGER.info("Diamond Trim: Night Vision applied due to being in a cave or below level 45");
        }

        // Check if near Haste II beacon for instant mining of deepslate
        if (isNearHaste2Beacon(player)) {
            BlockPos blockPos = player.getBlockPos().down();
            BlockState blockState = player.getWorld().getBlockState(blockPos);

            if (isDeepslateVariant(blockState)) {
                // Simulate instant mining of deepslate by reducing hardness to match stone
                blockState.getBlock().setHardness(Blocks.STONE.getHardness());
                LOGGER.info("Diamond Trim: Instant mining applied for Deepslate due to Haste II beacon");
            }
        }
    }


    // Gold Trim Effects
    private static void applyGoldEffect(LivingEntity player) {
        // Always apply Haste II and Luck I
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 1, false, true));  // Haste II
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 300, 0, false, true));  // Constant Luck I

        // Check if near Haste II beacon for instant mining of deepslate
        if (isNearHaste2Beacon(player)) {
            BlockPos blockPos = player.getBlockPos().down();
            BlockState blockState = player.getWorld().getBlockState(blockPos);

            if (isDeepslateVariant(blockState)) {
                // Simulate instant mining of deepslate by reducing hardness to match stone
                blockState.getBlock().setHardness(Blocks.STONE.getHardness());
                LOGGER.info("Gold Trim: Instant mining applied for Deepslate due to Haste II beacon");
            }
        }
    }


    // Amethyst Trim Effects
    private static void applyAmethystEffect(LivingEntity player) {
        // Apply speed boost while sprinting
        if (player.isSprinting()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 2, false, true));  // Speed III when sprinting
            LOGGER.info("Amethyst Trim: Speed boost applied while sprinting");
        }

        // Apply damage reduction
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 0, false, true));  // Resistance I (10% damage reduction)
        LOGGER.info("Amethyst Trim: 10% damage reduction applied");
    }


    // Redstone Trim Effects (Enhanced)
    private static void applyRedstoneEffect(LivingEntity player) {
        if (player.hurtTime > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 0));  // Speed I for 15 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 0));  // Strength I for 15 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 0));  // Haste I for 15 seconds
        }
    }

    // Quartz Trim Effects
    private static void applyQuartzEffect(LivingEntity player) {
        if (player.getWorld().getRegistryKey() == World.NETHER) {  // Nether check
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 0, false, true));  // Regeneration I for 15 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 0, false, true));  // Resistance I for 15 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 1, false, true));  // Haste II for 15 seconds
        }
    }

    // Emerald Trim Effects
    private static void applyEmeraldEffect(LivingEntity player) {
        List<VillagerEntity> nearbyVillagers = player.getWorld().getEntitiesByClass(VillagerEntity.class,
            player.getBoundingBox().expand(10), entity -> entity instanceof VillagerEntity);

        if (!nearbyVillagers.isEmpty()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 300, 0, false, true));  // Hero of the Village for 15 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 0, false, true));  // Resistance I for 15 seconds
        }
    }

    // Lapis Trim Effects
    private static void applyLapisEffect(LivingEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 300, 1, false, true));  // Luck II

        if (player.getAir() < player.getMaxAir()) {  // Underwater check
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 300, 0, false, true));  // Dolphin's Grace
        }

        if (player instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) player;
            int xpBonus = (int) (playerEntity.experienceProgress * 0.50 * playerEntity.getNextLevelExperience());
            playerEntity.addExperience(xpBonus); // XP Boost
        }
    }

    // Copper Trim Effects
    private static void applyCopperEffect(LivingEntity player) {
        if (player.getWorld().isThundering() && player.getWorld().random.nextFloat() < 0.05f) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1, false, true));  // Strength II
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1, false, true));  // Speed II
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 600, 1, false, true));  // Haste II for 5 minutes
    }

    // Iron Trim Effects
    private static void applyIronEffect(LivingEntity player) {
        if (player.getHealth() < 6.0F) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 0, false, true));  // Resistance I
        }
        if (player.getHealth() < (player.getMaxHealth() / 2)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 0, false, true));  // Strength
        }
        if (player.getBlockY() < 64) {  // Underground check
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 200, 0, false, true));  // Haste I
        }
    }

    // Netherite Trim Effects
    private static void applyNetheriteEffect(LivingEntity player) {
        if (player.isInLava() || player.isOnFire()) {
            player.heal(1.0F);  // Heal 1 health point
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 300, 0, false, true));  // Fire Resistance
        }
    }
}
