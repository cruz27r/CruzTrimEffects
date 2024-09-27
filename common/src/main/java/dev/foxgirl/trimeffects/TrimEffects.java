package dev.foxgirl.trimeffects;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PiglinEntity;
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
            case "minecraft:lapis_lazuli":
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
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0));  // Speed I for 5 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 100, 0));  // Strength I for 5 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, 0));  // Haste I for 3 seconds
        }
    }

    // Quartz Trim Effects (New)
    private static void applyQuartzEffect(LivingEntity player) {
        if (player.getWorld().getRegistryKey() == World.NETHER) {  // Nether check
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));  // Regeneration I for 10 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 600, 0));  // Fire Resistance for 30 seconds
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 0));  // Strength I in the Nether
        }
    }

    // Emerald Trim Effects (New)
    private static void applyEmeraldEffect(LivingEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 600, 0));  // Hero of the Village
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));  // Resistance I for 5 seconds when trading
    }

    // Lapis Trim Effects (Improved XP, Luck, and Dolphin's Grace)
    private static void applyLapisEffect(LivingEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 600, 0));  // Luck I for 30 seconds
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 600, 0));  // Dolphin's Grace for swimming

        // Give XP bonus (25% boost)
//        if (player.getWorld() instanceof ServerWorld) {
//            int xpBonus = (int) (player.totalExperience * 0.25);  // 25% XP boost
//            player.addExperience(xpBonus);
//        }
    }

    // Copper Trim Effects
    private static void applyCopperEffect(LivingEntity player) {
        if (player.getWorld().isThundering() && player.getWorld().random.nextFloat() < 0.05f) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 1));  // Strength I for 30 seconds
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));  // Resistance I for 10 seconds
    }

    // Iron Trim Effects
    private static void applyIronEffect(LivingEntity player) {
        if (player.getHealth() < 6.0F) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0));  // Resistance I
        }
    }

    // Diamond Trim Effects
    private static void applyDiamondEffect(LivingEntity player) {
        if (player.getBlockY() < 63) {  // Check if player is underground
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 2400, 0));  // Night Vision for 2 minutes
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 2400, 1));  // Haste II
    }

    // Netherite Trim Effects
    private static void applyNetheriteEffect(LivingEntity player) {
        if (player.isInLava() || player.isOnFire()) {
            player.heal(1.0F);  // Heal 1 health point (half a heart)
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));  // Resistance II for 10 seconds
        }
    }

    // Gold Trim Effects
    private static void applyGoldEffect(LivingEntity player) {
        // Get all nearby Piglins within 10 blocks
        List<PiglinEntity> nearbyPiglins = player.getWorld().getEntitiesByClass(PiglinEntity.class,
            player.getBoundingBox().expand(10), entity -> entity instanceof PiglinEntity);

        // For each Piglin, make sure they stop attacking
        for (PiglinEntity piglin : nearbyPiglins) {
            // Get Piglin's brain and reset its attack target
            var piglinBrain = piglin.getBrain();
            if (piglinBrain != null) {
                piglinBrain.forget(MemoryModuleType.ATTACK_TARGET);
                piglinBrain.remember(MemoryModuleType.ADMIRING_ITEM, true);  // Piglins admire the gold armor
            }

            // Prevent Piglins from attacking the player
            piglin.setAttacking(null);
        }
    }
}
