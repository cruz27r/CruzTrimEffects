package dev.foxgirl.trimeffects;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

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
        } else {
            removeAllEffects(player); // Remove effects if the full set isn't worn
        }
    }

    // Method to remove all effects applied by armor
    private void removeAllEffects(LivingEntity player) {
        player.removeStatusEffect(StatusEffects.SPEED);
        player.removeStatusEffect(StatusEffects.STRENGTH);
        player.removeStatusEffect(StatusEffects.HASTE);
        player.removeStatusEffect(StatusEffects.REGENERATION);
        player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        player.removeStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        player.removeStatusEffect(StatusEffects.LUCK);
        player.removeStatusEffect(StatusEffects.DOLPHINS_GRACE);
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
            default:
                break;
        }
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

    // Emerald Trim Effects (Hero of the Village only when near a Villager)
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
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 300, 1, false, true));  // Luck II, no particles, icon on

        if (player.getAir() < player.getMaxAir()) {  // If the player is underwater
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 300, 0, false, true));  // Dolphin's Grace, no particles, icon on
        }

        // 50% XP boost for Lapis trim
        if (player instanceof PlayerEntity) {
            PlayerEntity playerEntity = (PlayerEntity) player;
            int xpBonus = (int) (playerEntity.experienceProgress * 0.50 * playerEntity.getNextLevelExperience());
            playerEntity.addExperience(xpBonus);
        }
    }

    // Copper Trim Effects
    private static void applyCopperEffect(LivingEntity player) {
        if (player.getWorld().isThundering() && player.getWorld().random.nextFloat() < 0.05f) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1, false, true));  // Strength II, no particles, icon on
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1, false, true));  // Speed II, no particles, icon on
    }

    // Iron Trim Effects
    private static void applyIronEffect(LivingEntity player) {
        if (player.getHealth() < 6.0F) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 0, false, true));  // Resistance I, no particles, icon on
        }
        if (player.getHealth() < (player.getMaxHealth() / 2)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 0, false, true));  // Strength, no particles, icon on
        }
        if (player.getBlockY() < 64) {  // Underground
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 200, 0, false, true));  // Haste I, no particles, icon on
        }
    }

    // Diamond Trim Effects
    private static void applyDiamondEffect(LivingEntity player) {
        if (player.getBlockY() < 63) {  // Underground
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, true));  // Night Vision, no particles, icon on
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 300, 1, false, true));  // Haste II, no particles, icon on
    }

    // Netherite Trim Effects
    private static void applyNetheriteEffect(LivingEntity player) {
        if (player.isInLava() || player.isOnFire()) {
            player.heal(1.0F);  // Heal 1 health point
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 300, 0, false, true));  // Fire Resistance, no particles, icon on
        }
    }

    // Gold Trim Effects
    private static void applyGoldEffect(LivingEntity player) {
        List<PiglinEntity> nearbyPiglins = player.getWorld().getEntitiesByClass(PiglinEntity.class,
            player.getBoundingBox().expand(10), entity -> entity instanceof PiglinEntity);

        for (PiglinEntity piglin : nearbyPiglins) {
            var piglinBrain = piglin.getBrain();
            if (piglinBrain != null) {
                piglinBrain.forget(MemoryModuleType.ATTACK_TARGET);
                piglinBrain.remember(MemoryModuleType.ADMIRING_ITEM, true);  // Piglins admire the gold armor
            }

            piglin.setAttacking(null);  // Prevent Piglins from attacking the player
        }

        if (player instanceof PlayerEntity) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 300, 0, false, true));  // Luck I, no particles, icon on
        }
    }
}
