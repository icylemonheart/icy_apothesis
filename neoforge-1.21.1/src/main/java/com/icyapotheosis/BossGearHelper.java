package com.icyapotheosis;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.mobs.types.Invader;
import dev.shadowsoffire.apotheosis.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.socket.SocketedGems;
import dev.shadowsoffire.apotheosis.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.tiers.GenContext;
import dev.shadowsoffire.apotheosis.tiers.WorldTier;
import dev.shadowsoffire.apotheosis.util.NameHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import dev.shadowsoffire.placebo.systems.gear.GearSet;
import dev.shadowsoffire.placebo.systems.gear.GearSetRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = IcyApotheosis.MOD_ID)
public class BossGearHelper {

    private static final int DEFER_TICKS = 5;
    private static final Map<UUID, DeferredGearInfo> deferredBosses = new ConcurrentHashMap<>();
    private static long currentTick = 0;

    private record DeferredGearInfo(String rarityStr, String bossName, int color, long registerTick) {}

    
    public static void registerDeferred(Mob mob, String rarityStr, String bossName, int color) {
        deferredBosses.put(mob.getUUID(), new DeferredGearInfo(rarityStr, bossName, color, currentTick));
        IcyApotheosis.LOGGER.info("[icy_apotheosis] Registered deferred gear enhancement for {}", mob.getType());
    }

    
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        currentTick++;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, DeferredGearInfo>> it = deferredBosses.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DeferredGearInfo> entry = it.next();
            DeferredGearInfo info = entry.getValue();

            if (currentTick - info.registerTick() < DEFER_TICKS) continue;

            Mob mob = findMob(server, entry.getKey());
            if (mob == null || !mob.isAlive()) {
                it.remove();
                continue;
            }

            IcyApotheosis.LOGGER.info("[icy_apotheosis] Processing deferred gear enhancement for {}", mob.getType());
            applyGear(mob, info.rarityStr(), info.bossName(), info.color(), mob.getRandom());
            
            
            BossHandler.reapplyBossEffects(mob, info.rarityStr());
            it.remove();
        }
    }

    private static Mob findMob(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob) return mob;
        }
        return null;
    }

    
    private static GenContext makeBossGenContext(Mob mob, RandomSource rand) {
        Holder<Biome> biome = mob.level().getBiome(mob.blockPosition());
        return new GenContext(rand, WorldTier.PINNACLE, 0, mob.level().dimension(), biome, Set.of());
    }

    public static void applyGear(Mob mob, String rarityStr, String bossName, int color, RandomSource random) {
        try {
            DynamicHolder<LootRarity> rarityHolder = findRarityByPath(rarityStr);
            if (rarityHolder == null || !rarityHolder.isBound()) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] Rarity not found or not bound: {}", rarityStr);
                setBasicBossTags(mob, rarityStr);
                return;
            }

            LootRarity rarity = rarityHolder.get();

            
            LootRarity affixRarity = getAffixCompatibleRarity(rarity);

            
            boolean hasExistingEquipment = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!mob.getItemBySlot(slot).isEmpty()) {
                    hasExistingEquipment = true;
                    break;
                }
            }

            if (hasExistingEquipment) {
                IcyApotheosis.LOGGER.info("[icy_apotheosis] Detected existing equipment on {}, enhancing", mob.getType());
                enhanceExistingGear(mob, rarity, affixRarity, bossName, random);
            } else {
                IcyApotheosis.LOGGER.debug("[icy_apotheosis] No existing equipment, applying GearSet");
                applyNewGearSet(mob, rarity, affixRarity, bossName, random);
            }

            mob.getPersistentData().putBoolean("apoth.boss", true);
            ResourceLocation rarityId = RarityRegistry.INSTANCE.getKey(rarity);
            mob.getPersistentData().putString("apoth.rarity", rarityId.toString());

        } catch (Exception e) {
            IcyApotheosis.LOGGER.error("[icy_apotheosis] Error applying Apotheosis gear: {} -- CLASS={}",
                    e.getMessage(), e.getClass().getSimpleName(), e);
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            IcyApotheosis.LOGGER.error("[icy_apotheosis] FULL STACKTRACE:\n{}", sw.toString());
            setBasicBossTags(mob, rarityStr);
        }
    }

    
    private static DynamicHolder<LootRarity> findRarityByPath(String path) {
        boolean fullId = path.contains(":");
        DynamicHolder<LootRarity> fallback = null;
        for (LootRarity r : RarityRegistry.INSTANCE.getValues()) {
            ResourceLocation key = RarityRegistry.INSTANCE.getKey(r);
            if (key == null) continue;
            if (fullId) {
                if (key.toString().equals(path)) return RarityRegistry.INSTANCE.holder(r);
            } else {
                if (key.getPath().equals(path)) {
                    
                    if ("apotheosis".equals(key.getNamespace())) {
                        return RarityRegistry.INSTANCE.holder(r);
                    }
                    if (fallback == null) fallback = RarityRegistry.INSTANCE.holder(r);
                }
            }
        }
        return fallback; 
    }

    
    private static LootRarity getAffixCompatibleRarity(LootRarity targetRarity) {
        int ord = targetRarity.sortIndex();
        if (ord <= 700) return targetRarity; 
        
        DynamicHolder<LootRarity> arAncient = findRarityByPath("ancientreforging:ancient");
        if (arAncient != null && arAncient.isBound()) {
            return arAncient.get();
        }
        
        DynamicHolder<LootRarity> mythicHolder = findRarityByPath("mythic");
        if (mythicHolder != null && mythicHolder.isBound()) {
            return mythicHolder.get();
        }
        return targetRarity;
    }

    
    private static LootCategory resolveLootCategory(ItemStack stack) {
        LootCategory cat = LootCategory.forItem(stack);
        if (!cat.isNone()) return cat;

        
        net.minecraft.world.item.Item item = stack.getItem();
        net.minecraft.world.entity.EquipmentSlot slot = stack.getEquipmentSlot();

        
        if (item instanceof net.minecraft.world.item.ArmorItem armor) {
            net.minecraft.world.entity.EquipmentSlot armorSlot = armor.getEquipmentSlot();
            if (armorSlot == net.minecraft.world.entity.EquipmentSlot.HEAD) return Apoth.LootCategories.HELMET;
            if (armorSlot == net.minecraft.world.entity.EquipmentSlot.CHEST) return Apoth.LootCategories.CHESTPLATE;
            if (armorSlot == net.minecraft.world.entity.EquipmentSlot.LEGS) return Apoth.LootCategories.LEGGINGS;
            if (armorSlot == net.minecraft.world.entity.EquipmentSlot.FEET) return Apoth.LootCategories.BOOTS;
        }

        
        if (slot != null) {
            if (slot == net.minecraft.world.entity.EquipmentSlot.HEAD) return Apoth.LootCategories.HELMET;
            if (slot == net.minecraft.world.entity.EquipmentSlot.CHEST) return Apoth.LootCategories.CHESTPLATE;
            if (slot == net.minecraft.world.entity.EquipmentSlot.LEGS) return Apoth.LootCategories.LEGGINGS;
            if (slot == net.minecraft.world.entity.EquipmentSlot.FEET) return Apoth.LootCategories.BOOTS;
        }

        
        if (item instanceof net.minecraft.world.item.SwordItem || item instanceof net.minecraft.world.item.AxeItem) {
            return Apoth.LootCategories.MELEE_WEAPON;
        }
        if (item instanceof net.minecraft.world.item.PickaxeItem || item instanceof net.minecraft.world.item.ShovelItem) {
            return Apoth.LootCategories.BREAKER;
        }
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) {
            return Apoth.LootCategories.BOW;
        }
        if (item instanceof net.minecraft.world.item.TridentItem) {
            return Apoth.LootCategories.TRIDENT;
        }

        IcyApotheosis.LOGGER.warn("[icy_apotheosis] resolveLootCategory: item={} fell back to NONE", item);
        return cat;
    }

    
    private static ItemStack makeDefaultGearForSlot(EquipmentSlot s, LootRarity rarity) {
        boolean highTier = rarity.sortIndex() >= 750;
        return switch (s) {
            case HEAD -> new ItemStack(highTier ? net.minecraft.world.item.Items.NETHERITE_HELMET : net.minecraft.world.item.Items.DIAMOND_HELMET);
            case CHEST -> new ItemStack(highTier ? net.minecraft.world.item.Items.NETHERITE_CHESTPLATE : net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            case LEGS -> new ItemStack(highTier ? net.minecraft.world.item.Items.NETHERITE_LEGGINGS : net.minecraft.world.item.Items.DIAMOND_LEGGINGS);
            case FEET -> new ItemStack(highTier ? net.minecraft.world.item.Items.NETHERITE_BOOTS : net.minecraft.world.item.Items.DIAMOND_BOOTS);
            case MAINHAND -> new ItemStack(highTier ? net.minecraft.world.item.Items.NETHERITE_SWORD : net.minecraft.world.item.Items.DIAMOND_SWORD);
            case OFFHAND -> new ItemStack(net.minecraft.world.item.Items.SHIELD);
            default -> ItemStack.EMPTY;
        };
    }

    
    private static void fillEmptySlotsWithGear(Mob mob, LootRarity rarity, LootRarity affixRarity, String bossName,
                                               RandomSource random, GenContext bossCtx, int enchantLvl,
                                               RegistryAccess registryAccess) {
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            if (!mob.getItemBySlot(s).isEmpty()) continue;

            ItemStack gear = makeDefaultGearForSlot(s, rarity);
            if (gear == null || gear.isEmpty()) continue;

            IcyApotheosis.LOGGER.info("[icy_apotheosis] FILL empty slot={} with={} rarity={}", s, gear.getItem(), rarity);

            try {
                LootCategory cat = resolveLootCategory(gear);
                IcyApotheosis.LOGGER.info("[icy_apotheosis]   FILL slot={} resolveCat={} affixRarity={}", s, cat, affixRarity);

                if (!cat.isNone()) {
                    try {
                        gear = LootController.createLootItem(gear, cat, affixRarity, bossCtx);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.warn("[icy_apotheosis]   FILL createLootItem failed slot={}: {} — compensating",
                                s, e.getMessage());
                    }
                    IcyApotheosis.LOGGER.info("[icy_apotheosis]   FILL slot={} after createLootItem affixes={}",
                            s, AffixHelper.getAffixes(gear).size());
                    try {
                        compensateAffixes(gear, rarity, affixRarity, random);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.error("[icy_apotheosis]   FILL compensateAffixes CRASH slot={}: {}", s, e.getMessage());
                        logStackTrace("fillEmpty_compensate_" + s, e);
                    }
                }
                try {
                    socketGems(gear, rarity, random, bossCtx);
                } catch (Exception e) {
                    IcyApotheosis.LOGGER.error("[icy_apotheosis]   FILL socketGems CRASH slot={}: {}", s, e.getMessage());
                    logStackTrace("fillEmpty_socketGems_" + s, e);
                }
                try {
                    updateNameColor(gear, rarity);
                } catch (Exception e) {
                    IcyApotheosis.LOGGER.error("[icy_apotheosis]   FILL updateNameColor CRASH slot={}: {}", s, e.getMessage());
                    logStackTrace("fillEmpty_updateNameColor_" + s, e);
                }
            } catch (Exception e) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] FILL affix CRASH slot={}: {} CLASS={}",
                        s, e.getMessage(), e.getClass().getSimpleName());
                logStackTrace("fillEmptySlot_" + s, e);
            }

            
            boolean isStillPlain = AffixHelper.getAffixes(gear).isEmpty()
                    && net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(gear).entrySet().isEmpty();
            if (gear.isEnchantable() && isStillPlain) {
                try {
                    Invader.enchantBossItem(random, gear, enchantLvl, true, registryAccess);
                } catch (Exception ignore) {
                    
                }
            }

            mob.setItemSlot(s, gear);
            IcyApotheosis.LOGGER.info("[icy_apotheosis]   FILL slot={} DONE affixes={} enchants={}", s,
                    AffixHelper.getAffixes(gear).size(),
                    net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentsForCrafting(gear).size());
        }
    }

    
    private static void enhanceExistingGear(Mob mob, LootRarity rarity, LootRarity affixRarity, String bossName, RandomSource random) {
        int enchantLvl = Math.max(10, rarity.sortIndex() * 10 + 10);
        RegistryAccess registryAccess = mob.level().registryAccess();
        GenContext bossCtx = makeBossGenContext(mob, random);

        List<EquipmentSlot> validSlots = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            
            if (!stack.isEmpty() && !resolveLootCategory(stack).isNone()) {
                validSlots.add(slot);
            }
        }

        if (validSlots.isEmpty()) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] Existing equipment has no valid loot categories, falling back to GearSet");
            applyNewGearSet(mob, rarity, affixRarity, bossName, random);
            return;
        }

        EquipmentSlot guaranteedSlot = validSlots.get(random.nextInt(validSlots.size()));

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            boolean enchanted = false;
            ItemStack savedEnchants = stack.copy();
            ItemStack clean = null;

            
            LootCategory cat = resolveLootCategory(stack);
            if (!cat.isNone()) {
                
                clean = stack.copy();
                clean.setCount(1);
                clean.setDamageValue(stack.getDamageValue());

                LootCategory origCat = LootCategory.forItem(stack);
                LootCategory forItemClean = LootCategory.forItem(clean);
                IcyApotheosis.LOGGER.info("[icy_apotheosis] ENHANCE slot={} item={} forItemOrig={} resolveCat={} forItemClean={} affixRarity={}",
                        slot, stack.getItem(), origCat, cat, forItemClean, affixRarity);

                
                for (AffixType at : new AffixType[]{AffixType.STAT, AffixType.ABILITY}) {
                    long countNative = LootController.getAvailableAffixes(clean, affixRarity, at).count();
                    long countSafe = getAvailableAffixesSafe(clean, cat, affixRarity, at).count();
                    IcyApotheosis.LOGGER.info("[icy_apotheosis]   pre-check type={} nativeAvail={} safeAvail={}", at, countNative, countSafe);
                }

                try {
                    
                    
                    
                    clean = LootController.createLootItem(clean, resolveLootCategory(clean), affixRarity, bossCtx);
                } catch (Exception e) {
                    IcyApotheosis.LOGGER.warn("[icy_apotheosis] createLootItem FAILED slot={} item={}: {} — will compensate",
                            slot, stack.getItem(), e.getMessage());
                }

                IcyApotheosis.LOGGER.info("[icy_apotheosis] after createLootItem slot={} affixes={} rarity={}",
                        slot, AffixHelper.getAffixes(clean).size(), clean.get(Apoth.Components.RARITY));

                
                
                compensateAffixes(clean, rarity, affixRarity, random);

                
                ItemEnchantments savedEnch = EnchantmentHelper.getEnchantmentsForCrafting(savedEnchants);
                ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(clean));
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : savedEnch.entrySet()) {
                    Holder<Enchantment> ench = entry.getKey();
                    int other = entry.getIntValue();
                    int cur = merged.getLevel(ench);
                    if (other > cur) merged.set(ench, other);
                }
                EnchantmentHelper.setEnchantments(clean, merged.toImmutable());

                socketGems(clean, rarity, random, bossCtx);
                updateNameColor(clean, rarity);

                mob.setItemSlot(slot, clean);
                stack = clean;
                enchanted = true;
            }

            if (!enchanted && stack.isEnchantable()) {
                Invader.enchantBossItem(random, stack, enchantLvl, true, registryAccess);
                mob.setItemSlot(slot, stack);
            }

            if (slot == guaranteedSlot) {
                mob.setDropChance(slot, 2.0f);
                modifyBossItem(stack, bossName, rarity, random, registryAccess);
                mob.setItemSlot(slot, stack);
            } else if (!enchanted && random.nextFloat() < 0.5f && stack.isEnchantable()) {
                Invader.enchantBossItem(random, stack, enchantLvl, true, registryAccess);
                mob.setItemSlot(slot, stack);
            }
        }

        
        fillEmptySlotsWithGear(mob, rarity, affixRarity, bossName, random, bossCtx, enchantLvl, registryAccess);
    }

    
    private static void applyNewGearSet(Mob mob, LootRarity rarity, LootRarity affixRarity, String bossName, RandomSource random) {
        GearSet set = GearSetRegistry.INSTANCE.getRandomSet(random, 0f, null);
        RegistryAccess registryAccess = mob.level().registryAccess();
        GenContext bossCtx = makeBossGenContext(mob, random);
        int enchantLvl = Math.max(10, rarity.sortIndex() * 10 + 10);

        if (set == null) {
            IcyApotheosis.LOGGER.error("[icy_apotheosis] GearSet is null, filling all 6 slots with default gear");
            
            fillEmptySlotsWithGear(mob, rarity, affixRarity, bossName, random, bossCtx, enchantLvl, registryAccess);
            return;
        }

        set.apply(mob);



        int guaranteed = random.nextInt(6);

        
        boolean anyValid = false;
        for (EquipmentSlot t : EquipmentSlot.values()) {
            ItemStack s = mob.getItemBySlot(t);
            if (!s.isEmpty() && !resolveLootCategory(s).isNone()) {
                anyValid = true;
                break;
            }
        }

        
        
        if (!anyValid) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] applyNewGearSet !anyValid — forcing all slots via compensateAffixes");
            List<EquipmentSlot> validList = new ArrayList<>();
            for (EquipmentSlot s : EquipmentSlot.values()) {
                ItemStack stack = mob.getItemBySlot(s);
                if (!stack.isEmpty()) {
                    LootCategory resolveCat = resolveLootCategory(stack);
                    IcyApotheosis.LOGGER.info("[icy_apotheosis] !anyValid slot={} item={} forItemCat={} resolveCat={}",
                            s, stack.getItem(), LootCategory.forItem(stack), resolveCat);
                    try {
                        stack = LootController.createLootItem(stack, affixRarity, bossCtx);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.warn("[icy_apotheosis] !anyValid createLootItem failed slot={} item={}: {}", s, stack.getItem(), e.getMessage());
                    }
                    compensateAffixes(stack, rarity, affixRarity, random);
                    socketGems(stack, rarity, random, bossCtx);
                    updateNameColor(stack, rarity);
                    mob.setItemSlot(s, stack);
                    if (!resolveCat.isNone()) validList.add(s);
                }
            }
            if (validList.isEmpty()) validList.add(EquipmentSlot.values()[0]);
            EquipmentSlot gSlot = validList.get(random.nextInt(validList.size()));
            ItemStack gstack = mob.getItemBySlot(gSlot);
            if (!gstack.isEmpty() && gstack.isEnchantable()) {
                Invader.enchantBossItem(random, gstack, enchantLvl, true, registryAccess);
            }
            mob.setDropChance(gSlot, 2.0f);
            modifyBossItem(gstack, bossName, rarity, random, registryAccess);
            mob.setItemSlot(gSlot, gstack);
            
            for (EquipmentSlot s : EquipmentSlot.values()) {
                if (s == gSlot) continue;
                ItemStack stack = mob.getItemBySlot(s);
                if (stack.isEmpty() || !stack.isEnchantable()) continue;
                boolean hasAffixes = AffixHelper.getAffixes(stack).size() > 0;
                if (!hasAffixes && random.nextFloat() < 0.5f) {
                    Invader.enchantBossItem(random, stack, enchantLvl, true, registryAccess);
                    mob.setItemSlot(s, stack);
                }
            }
            
            fillEmptySlotsWithGear(mob, rarity, affixRarity, bossName, random, bossCtx, enchantLvl, registryAccess);
            return; 
        }

        
        ItemStack temp = mob.getItemBySlot(EquipmentSlot.values()[guaranteed]);
        while (temp.isEmpty() || resolveLootCategory(temp).isNone()) {
            guaranteed = random.nextInt(6);
            temp = mob.getItemBySlot(EquipmentSlot.values()[guaranteed]);
        }

        
        for (EquipmentSlot s : EquipmentSlot.values()) {
            try {
                ItemStack stack = mob.getItemBySlot(s);
                if (stack.isEmpty()) continue;

                boolean enchanted = false;

                
                LootCategory resolveCat = resolveLootCategory(stack);
                LootCategory origCat = LootCategory.forItem(stack);
                IcyApotheosis.LOGGER.info("[icy_apotheosis] applyNewGearSet MAIN slot={} item={} forItemCat={} resolveCat={} affixRarity={}",
                        s, stack.getItem(), origCat, resolveCat, affixRarity);

                if (!resolveCat.isNone()) {
                    try {
                        stack = LootController.createLootItem(stack, resolveCat, affixRarity, bossCtx);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.warn("[icy_apotheosis] applyNewGearSet createLootItem failed slot={} item={}: {} — compensating",
                                s, stack.getItem(), e.getMessage());
                    }

                    IcyApotheosis.LOGGER.info("[icy_apotheosis] after createLootItem MAIN slot={} affixes={} rarity={}",
                            s, AffixHelper.getAffixes(stack).size(), stack.get(Apoth.Components.RARITY));

                    try {
                        compensateAffixes(stack, rarity, affixRarity, random);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.error("[icy_apotheosis] compensateAffixes CRASH slot={} item={}: {} CLASS={}",
                                s, stack.getItem(), e.getMessage(), e.getClass().getSimpleName());
                        logStackTrace("compensateAffixes_" + s, e);
                    }
                    try {
                        socketGems(stack, rarity, random, bossCtx);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.error("[icy_apotheosis] socketGems CRASH slot={} item={}: {} CLASS={}",
                                s, stack.getItem(), e.getMessage(), e.getClass().getSimpleName());
                        logStackTrace("socketGems_" + s, e);
                    }
                    try {
                        updateNameColor(stack, rarity);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.error("[icy_apotheosis] updateNameColor CRASH slot={} item={}: {} CLASS={}",
                                s, stack.getItem(), e.getMessage(), e.getClass().getSimpleName());
                        logStackTrace("updateNameColor_" + s, e);
                    }
                    try {
                        mob.setItemSlot(s, stack);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.error("[icy_apotheosis] setItemSlot CRASH slot={} item={}: {} CLASS={}",
                                s, stack.getItem(), e.getMessage(), e.getClass().getSimpleName());
                        logStackTrace("setItemSlot_" + s, e);
                    }
                    enchanted = true;
                }

                if (!enchanted && stack.isEnchantable()) {
                    Invader.enchantBossItem(random, stack, enchantLvl, true, registryAccess);
                    mob.setItemSlot(s, stack);
                }

                if (s.ordinal() == guaranteed) {
                    mob.setDropChance(s, 2.0f);
                    modifyBossItem(stack, bossName, rarity, random, registryAccess);
                    mob.setItemSlot(s, stack);
                } else if (!enchanted && random.nextFloat() < 0.5f && stack.isEnchantable()) {
                    Invader.enchantBossItem(random, stack, enchantLvl, true, registryAccess);
                    mob.setItemSlot(s, stack);
                }
            } catch (Exception slotEx) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] SLOT CRASH (applyNewGearSet) slot={}: {} CLASS={}",
                        s, slotEx.getMessage(), slotEx.getClass().getSimpleName());
                logStackTrace("applyNewGearSet_SLOT_" + s, slotEx);
            }
        }

        
        fillEmptySlotsWithGear(mob, rarity, affixRarity, bossName, random, bossCtx, enchantLvl, registryAccess);
    }

    private static void setBasicBossTags(Mob mob, String rarityStr) {
        mob.getPersistentData().putBoolean("apoth.boss", true);
        mob.getPersistentData().putString("apoth.rarity", "apotheosis:" + rarityStr);
    }

    
    private static void socketGems(ItemStack stack, LootRarity bossRarity, RandomSource random, GenContext ctx) {
        if (bossRarity.sortIndex() < 600) return; 

        int sockets = SocketHelper.getSockets(stack);
        if (sockets <= 0) return;

        
        LootCategory cat = resolveLootCategory(stack);
        if (cat.isNone()) return;

        List<GemInstance> gems = new ArrayList<>(sockets);
        for (int i = 0; i < sockets; i++) gems.add(GemInstance.EMPTY);

        for (int slot = 0; slot < sockets; slot++) {
            try {
                ItemStack gemStack = GemRegistry.createRandomGemStack(ctx);
                if (gemStack == null || gemStack.isEmpty()) continue;
                
                GemInstance inst = GemInstance.socketed(cat, gemStack, slot);
                if (inst.isValid()) {
                    gems.set(slot, inst);
                }
            } catch (Exception e) {
                IcyApotheosis.LOGGER.warn("[icy_apotheosis] socketGems slot={} failed: {}", slot, e.getMessage());
            }
        }

        SocketHelper.setGems(stack, new SocketedGems(gems));
    }

    
    private static record PoolResult(List<DynamicHolder<Affix>> pool,
                                     DynamicHolder<LootRarity> usedRarity,
                                     int usedOrdinal) {}

    
    private static PoolResult getPoolWithRarityFallback(ItemStack stack,
                                                        LootRarity affixRarity,
                                                        Set<DynamicHolder<Affix>> selected,
                                                        AffixType type) {
        
        LootCategory cat = resolveLootCategory(stack);

        List<LootRarity> sorted = RarityRegistry.getSortedRarities();

        
        int startIndex = 0;
        if (affixRarity != null) {
            int idx = sorted.indexOf(affixRarity);
            if (idx >= 0) {
                startIndex = idx;
            }
        }

        
        for (int i = startIndex; i >= 0; i--) {
            LootRarity r = sorted.get(i);
            DynamicHolder<LootRarity> holder = RarityRegistry.INSTANCE.holder(r);
            if (holder == null || !holder.isBound()) continue;
            
            List<DynamicHolder<Affix>> pool = getAvailableAffixesSafe(stack, cat, r, type)
                .filter(h -> !selected.contains(h))
                .toList();
            if (!pool.isEmpty()) {
                return new PoolResult(pool, holder, r.sortIndex());
            }
        }

        
        DynamicHolder<LootRarity> empty = (affixRarity != null)
            ? RarityRegistry.INSTANCE.holder(affixRarity)
            : RarityRegistry.INSTANCE.emptyHolder();
        return new PoolResult(Collections.emptyList(), empty, affixRarity == null ? 0 : affixRarity.sortIndex());
    }

    
    private static java.util.stream.Stream<DynamicHolder<Affix>> getAvailableAffixesSafe(
            ItemStack stack, LootCategory cat, LootRarity rarity, AffixType type) {
        
        LootCategory nativeCat = LootCategory.forItem(stack);
        if (!nativeCat.isNone()) {
            return LootController.getAvailableAffixes(stack, rarity, type);
        }

        
        
        if (cat.isNone()) {
            return LootController.getAvailableAffixes(stack, rarity, type);
        }

        
        
        dev.shadowsoffire.apotheosis.affix.ItemAffixes current = stack.getOrDefault(
                Apoth.Components.AFFIXES, dev.shadowsoffire.apotheosis.affix.ItemAffixes.EMPTY);
        return AffixHelper.byType(type).stream()
            .filter(a -> a.isBound())
            .filter(a -> a.get().canApplyTo(stack, cat, rarity))
            .filter(a -> a.get().isCompatibleWith(current));
    }

    
    private static void compensateAffixes(ItemStack stack, LootRarity displayRarity, LootRarity affixRarity, RandomSource rand) {
        int ord = displayRarity == null ? 0 : displayRarity.sortIndex();
        final int minStat, minAbility, minSocket;
        if (ord >= 900) { minStat = 7; minAbility = 5; minSocket = 6; }
        else if (ord >= 800) { minStat = 6; minAbility = 4; minSocket = 5; }
        else if (ord >= 750) { minStat = 5; minAbility = 3; minSocket = 4; }
        else if (ord >= 700) { minStat = 4; minAbility = 2; minSocket = 3; }
        else if (ord >= 600) { minStat = 3; minAbility = 1; minSocket = 2; }
        else return;

        int safety;

        
        safety = 0;
        while (safety++ < 50) {
            var map = AffixHelper.getAffixes(stack);
            int statN = 0, abilityN = 0;
            Set<DynamicHolder<Affix>> selected = new HashSet<>();
            for (var e : map.entrySet()) {
                AffixType t = e.getValue().affix().get().definition().type();
                if (t == AffixType.STAT) statN++;
                else if (t == AffixType.ABILITY || t == AffixType.BASIC_EFFECT) abilityN++;
                selected.add(e.getKey());
            }
            if (statN >= minStat) break;

            
            AffixType[] tryOrder = { AffixType.STAT, AffixType.ABILITY, AffixType.BASIC_EFFECT };
            DynamicHolder<LootRarity> fallbackHolder = affixRarity != null
                ? RarityRegistry.INSTANCE.holder(affixRarity)
                : RarityRegistry.INSTANCE.emptyHolder();
            PoolResult best = new PoolResult(Collections.emptyList(),
                    fallbackHolder, affixRarity == null ? 0 : affixRarity.sortIndex());
            AffixType usedType = AffixType.STAT;
            for (AffixType t : tryOrder) {
                PoolResult pr = getPoolWithRarityFallback(stack, affixRarity, selected, t);
                if (!pr.pool().isEmpty()) {
                    best = pr;
                    usedType = t;
                    break;
                }
            }

            if (best.pool().isEmpty()) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] compensateAffixes: ALL rarities/types pool exhausted for STAT " +
                        "at {}/{} (item={}, LootCategory={})", statN, minStat, stack.getItem(), resolveLootCategory(stack));
                break;
            }

            Collections.shuffle(best.pool(), new java.util.Random(rand.nextLong()));
            DynamicHolder<Affix> chosen = best.pool().get(0);
            float lvl = Math.max(0.4F, rand.nextFloat());
            AffixInstance inst = new AffixInstance(chosen, lvl, best.usedRarity(), stack);
            AffixHelper.applyAffix(stack, inst);
            IcyApotheosis.LOGGER.info("[icy_apotheosis] STAT+1 [{}/{}] type={} affix={} rarity@ord={} level={}",
                    statN + 1, minStat, usedType, chosen.getId(), best.usedOrdinal(), lvl);
        }

        
        safety = 0;
        while (safety++ < 50) {
            var map = AffixHelper.getAffixes(stack);
            int statN = 0, abilityN = 0;
            Set<DynamicHolder<Affix>> selected = new HashSet<>();
            for (var e : map.entrySet()) {
                AffixType t = e.getValue().affix().get().definition().type();
                if (t == AffixType.STAT) statN++;
                else if (t == AffixType.ABILITY || t == AffixType.BASIC_EFFECT) abilityN++;
                selected.add(e.getKey());
            }
            if (abilityN >= minAbility) break;

            AffixType[] tryOrder = { AffixType.ABILITY, AffixType.STAT, AffixType.BASIC_EFFECT };
            DynamicHolder<LootRarity> fallbackHolder = affixRarity != null
                ? RarityRegistry.INSTANCE.holder(affixRarity)
                : RarityRegistry.INSTANCE.emptyHolder();
            PoolResult best = new PoolResult(Collections.emptyList(),
                    fallbackHolder, affixRarity == null ? 0 : affixRarity.sortIndex());
            AffixType usedType = AffixType.ABILITY;
            for (AffixType t : tryOrder) {
                PoolResult pr = getPoolWithRarityFallback(stack, affixRarity, selected, t);
                if (!pr.pool().isEmpty()) {
                    best = pr;
                    usedType = t;
                    break;
                }
            }

            if (best.pool().isEmpty()) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] compensateAffixes: ALL rarities/types pool exhausted for ABILITY " +
                        "at {}/{} (item={}, LootCategory={})", abilityN, minAbility, stack.getItem(), resolveLootCategory(stack));
                break;
            }

            Collections.shuffle(best.pool(), new java.util.Random(rand.nextLong()));
            DynamicHolder<Affix> chosen = best.pool().get(0);
            float lvl = Math.max(0.4F, rand.nextFloat());
            AffixInstance inst = new AffixInstance(chosen, lvl, best.usedRarity(), stack);
            AffixHelper.applyAffix(stack, inst);
            IcyApotheosis.LOGGER.info("[icy_apotheosis] ABILITY+1 [{}/{}] type={} affix={} rarity@ord={} level={}",
                    abilityN + 1, minAbility, usedType, chosen.getId(), best.usedOrdinal(), lvl);
        }

        
        int curSockets = SocketHelper.getSockets(stack);
        if (curSockets < minSocket) {
            SocketHelper.setSockets(stack, minSocket);
            IcyApotheosis.LOGGER.info("[icy_apotheosis] compensateAffixes: sockets {} -> {} (item={})", curSockets, minSocket, stack.getItem());
        }

        
        var finalMap = AffixHelper.getAffixes(stack);
        int fStat = 0, fAb = 0;
        for (var e : finalMap.values()) {
            AffixType t = e.affix().get().definition().type();
            if (t == AffixType.STAT) fStat++;
            else if (t == AffixType.ABILITY || t == AffixType.BASIC_EFFECT) fAb++;
        }
        int fSock = SocketHelper.getSockets(stack);
        IcyApotheosis.LOGGER.info("[icy_apotheosis] ============================================================");
        IcyApotheosis.LOGGER.info("[icy_apotheosis] compensateAffixes DONE: display={} STAT={}/{} ABILITY={}/{} SOCKET={}/{} item={}",
                displayRarity, fStat, minStat, fAb, minAbility, fSock, minSocket, stack.getItem());
        IcyApotheosis.LOGGER.info("[icy_apotheosis] ============================================================");
    }

    private static void logStackTrace(String tag, Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        IcyApotheosis.LOGGER.error("[icy_apotheosis] STACKTRACE [{}]:\n{}", tag, sw.toString());
    }

    
    private static void updateNameColor(ItemStack stack, LootRarity colorRarity) {
        Component currentName = AffixHelper.getName(stack);
        if (currentName != null) {
            MutableComponent newName = currentName.copy().withStyle(s -> s.withColor(colorRarity.color()));
            AffixHelper.setName(stack, newName);
        }
    }

    private static void modifyBossItem(ItemStack stack, String bossName, LootRarity rarity, RandomSource random, RegistryAccess registryAccess) {
        try {
            int enchantLvl = Math.max(10, rarity.sortIndex() * 10 + 10);
            Invader.enchantBossItem(random, stack, enchantLvl, true, registryAccess);

            String ownershipFormat = NameHelper.ownershipFormat;
            String bossOwnerName = String.format(ownershipFormat, bossName);

            MutableComponent itemName = stack.getHoverName().copy();
            MutableComponent newName = Component.translatable("misc.apotheosis.affix_name.four",
                bossOwnerName, itemName);

            
            newName.withStyle(s -> s.withColor(rarity.color()));
            AffixHelper.setName(stack, newName);

            stack.set(Apoth.Components.FROM_BOSS, true);
        } catch (Exception e) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] Error modifying boss item: {}", e.getMessage());
            stack.set(Apoth.Components.FROM_BOSS, true);
        }
    }
}
