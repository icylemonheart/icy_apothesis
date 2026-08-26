package com.icyapotheosis;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.boss.ApothBoss;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.Gem;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemRegistry;
import dev.shadowsoffire.apotheosis.util.NameHelper;
import dev.shadowsoffire.placebo.json.GearSet;
import dev.shadowsoffire.placebo.json.GearSetRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = IcyApotheosis.MOD_ID)
public class BossGearHelper {

    private static final int DEFER_TICKS = 5;
    private static final Map<UUID, DeferredGearInfo> deferredBosses = new ConcurrentHashMap<>();
    private static long currentTick = 0;

    private record DeferredGearInfo(String rarityStr, String bossName, int color, long registerTick) {}

    
    private static Class<?> clazzTieredSocketHelper = null;
    private static java.lang.reflect.Method methodSetSocketTiers = null;
    private static boolean fgaAvailable = false;
    private static boolean fgaChecked = false;

    private static void ensureFGALookup() {
        if (fgaChecked) return;
        fgaChecked = true;
        fgaAvailable = false;
        try {
            clazzTieredSocketHelper = Class.forName("net.kayn.fallen_gems_affixes.adventure.socket.TieredSocketHelper");
            methodSetSocketTiers = clazzTieredSocketHelper.getMethod("setSocketTiers", ItemStack.class, int[].class);
            
            if (!java.lang.reflect.Modifier.isStatic(methodSetSocketTiers.getModifiers())) {
                IcyApotheosis.LOGGER.warn("[icy_apotheosis] Fallen-Gems-Affixes TieredSocketHelper.setSocketTiers is not static.");
                methodSetSocketTiers = null;
                return;
            }
            methodSetSocketTiers.setAccessible(true);
            fgaAvailable = true;
            IcyApotheosis.LOGGER.info("[icy_apotheosis] Fallen-Gems-Affixes detected, enabling tiered socket support.");
        } catch (ClassNotFoundException e) {
            IcyApotheosis.LOGGER.info("[icy_apotheosis] Fallen-Gems-Affixes not present, tiered socket support disabled.");
        } catch (NoSuchMethodException e) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] Fallen-Gems-Affixes API changed (setSocketTiers not found), tiered socket support disabled.");
        } catch (Throwable t) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] Failed to hook Fallen-Gems-Affixes: {}", t.toString());
        }
    }

    
    private static void forceMinimumSocketTier(ItemStack stack, LootRarity bossRarity) {
        try {
            ensureFGALookup();
            if (!fgaAvailable) return;
            if (methodSetSocketTiers == null) return;
            if (stack == null || stack.isEmpty()) return;

            int sockets = SocketHelper.getSockets(stack);
            if (sockets <= 0) return;

            int ord = bossRarity == null ? 0 : bossRarity.ordinal();
            int minTierOrdinal = Math.max(0, ord - 2);

            int[] tiers = new int[sockets];
            for (int i = 0; i < sockets; i++) tiers[i] = minTierOrdinal;

            Object ret = methodSetSocketTiers.invoke(null, stack, tiers);
            
            if (ret instanceof ItemStack retStack && retStack != stack) {
                
                IcyApotheosis.LOGGER.debug("[icy_apotheosis] TieredSocketHelper returned new stack instance.");
            }
        } catch (Throwable t) {
            
            Throwable cause = t instanceof java.lang.reflect.InvocationTargetException ? t.getCause() : t;
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] Failed to force socket tiers on {}: {}",
                    stack == null ? "null" : stack.getItem(),
                    cause == null ? t.toString() : cause.toString());
            
            fgaAvailable = false;
        }
    }

    
    public static void registerDeferred(Mob mob, String rarityStr, String bossName, int color) {
        deferredBosses.put(mob.getUUID(), new DeferredGearInfo(rarityStr, bossName, color, currentTick));
        IcyApotheosis.LOGGER.info("[icy_apotheosis] Registered deferred gear enhancement for {}", mob.getType());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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

    public static void applyGear(Mob mob, String rarityStr, String bossName, int color, RandomSource random) {
        try {
            DynamicHolder<LootRarity> rarityHolder = RarityRegistry.byLegacyId("apotheosis:" + rarityStr);
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
            IcyApotheosis.LOGGER.error("[icy_apotheosis] Error applying Apotheosis gear: {}", e.getMessage());
            e.printStackTrace();
            setBasicBossTags(mob, rarityStr);
        }
    }

    
    private static LootRarity getAffixCompatibleRarity(LootRarity targetRarity) {
        if (targetRarity.ordinal() <= 5) return targetRarity;
        
        DynamicHolder<LootRarity> ancientHolder = RarityRegistry.byLegacyId("apotheosis:ancient");
        if (ancientHolder != null && ancientHolder.isBound()) {
            return ancientHolder.get();
        }
        return targetRarity;
    }

    
    private static void enhanceExistingGear(Mob mob, LootRarity rarity, LootRarity affixRarity, String bossName, RandomSource random) {
        int enchantLvl = Math.max(10, rarity.ordinal() * 10 + 10);

        List<EquipmentSlot> validSlots = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (!stack.isEmpty() && !LootCategory.forItem(stack).isNone()) {
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

            if (!LootCategory.forItem(stack).isNone()) {
                clean = new ItemStack(stack.getItem(), 1);
                clean.setDamageValue(stack.getDamageValue());

                boolean createOk = false;
                try {
                    
                    clean = LootController.createLootItem(clean, affixRarity, random);
                    AffixHelper.setRarity(clean, rarity);
                    createOk = true;
                    IcyApotheosis.LOGGER.info("[icy_apotheosis] createLootItem OK slot={} item={}", slot, stack.getItem());
                } catch (Exception e) {
                    IcyApotheosis.LOGGER.warn("[icy_apotheosis] createLootItem FAILED slot={} item={}: {} — will compensate from empty.",
                            slot, stack.getItem(), e.getMessage());
                }

                
                if (!AffixHelper.hasAffixes(clean)) {
                    AffixHelper.setRarity(clean, rarity);
                }
                compensateAffixes(clean, rarity, affixRarity, random);

                
                for (var entry : savedEnchants.getAllEnchantments().entrySet()) {
                    int cur = clean.getEnchantmentLevel(entry.getKey());
                    int other = entry.getValue();
                    if (other > cur) clean.enchant(entry.getKey(), other);
                }

                forceMinimumSocketTier(clean, rarity);
                socketGems(clean, rarity, random);

                mob.setItemSlot(slot, clean);
                stack = clean;
                enchanted = true;
            }

            if (!enchanted && stack.isEnchantable()) {
                ApothBoss.enchantBossItem(random, stack, enchantLvl, true);
                mob.setItemSlot(slot, stack);
            }

            if (slot == guaranteedSlot) {
                mob.setDropChance(slot, 2.0f);
                modifyBossItem(stack, bossName, rarity, random);
                mob.setItemSlot(slot, stack);
            } else if (!enchanted && random.nextFloat() < 0.5f && stack.isEnchantable()) {
                ApothBoss.enchantBossItem(random, stack, enchantLvl, true);
                mob.setItemSlot(slot, stack);
            }
        }
    }

    
    private static void applyNewGearSet(Mob mob, LootRarity rarity, LootRarity affixRarity, String bossName, RandomSource random) {
        GearSet set = GearSetRegistry.INSTANCE.getRandomSet(random, 0f, null);
        if (set == null) {
            IcyApotheosis.LOGGER.error("[icy_apotheosis] GearSet is null");
            return;
        }

        set.apply(mob);

        int guaranteed = random.nextInt(6);

        boolean anyValid = false;
        for (EquipmentSlot t : EquipmentSlot.values()) {
            ItemStack s = mob.getItemBySlot(t);
            if (!s.isEmpty() && !LootCategory.forItem(s).isNone()) {
                anyValid = true;
                break;
            }
        }

        if (!anyValid) {
            for (EquipmentSlot s : EquipmentSlot.values()) {
                ItemStack stack = mob.getItemBySlot(s);
                if (!stack.isEmpty()) {
                    try {
                        LootController.createLootItem(stack, affixRarity, random);
                    } catch (Exception e) {
                        IcyApotheosis.LOGGER.warn("[icy_apotheosis] !anyValid createLootItem failed slot={} item={}: {}", s, stack.getItem(), e.getMessage());
                    }
                    if (!AffixHelper.hasAffixes(stack)) AffixHelper.setRarity(stack, rarity);
                    compensateAffixes(stack, rarity, affixRarity, random);
                    forceMinimumSocketTier(stack, rarity);
                    socketGems(stack, rarity, random);
                    mob.setItemSlot(s, stack);
                }
            }
        }

        ItemStack temp = mob.getItemBySlot(EquipmentSlot.values()[guaranteed]);
        while (temp.isEmpty() || LootCategory.forItem(temp).isNone()) {
            guaranteed = random.nextInt(6);
            temp = mob.getItemBySlot(EquipmentSlot.values()[guaranteed]);
        }

        int enchantLvl = Math.max(10, rarity.ordinal() * 10 + 10);

        for (EquipmentSlot s : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(s);
            if (stack.isEmpty()) continue;

            boolean enchanted = false;

            if (!LootCategory.forItem(stack).isNone()) {
                try {
                    stack = LootController.createLootItem(stack, affixRarity, random);
                } catch (Exception e) {
                    IcyApotheosis.LOGGER.warn("[icy_apotheosis] applyNewGearSet createLootItem failed slot={} item={}: {} — compensating from empty",
                            s, stack.getItem(), e.getMessage());
                }
                
                if (!AffixHelper.hasAffixes(stack)) {
                    AffixHelper.setRarity(stack, rarity);
                }
                compensateAffixes(stack, rarity, affixRarity, random);
                
                forceMinimumSocketTier(stack, rarity);
                
                socketGems(stack, rarity, random);
                mob.setItemSlot(s, stack);
                enchanted = true;
            }

            if (!enchanted && stack.isEnchantable()) {
                ApothBoss.enchantBossItem(random, stack, enchantLvl, true);
                mob.setItemSlot(s, stack);
            }

            if (s.ordinal() == guaranteed) {
                mob.setDropChance(s, 2.0f);
                modifyBossItem(stack, bossName, rarity, random);
                mob.setItemSlot(s, stack);
            } else if (!enchanted && random.nextFloat() < 0.5f && stack.isEnchantable()) {
                ApothBoss.enchantBossItem(random, stack, enchantLvl, true);
                mob.setItemSlot(s, stack);
            }
        }
    }

    private static void setBasicBossTags(Mob mob, String rarityStr) {
        mob.getPersistentData().putBoolean("apoth.boss", true);
        mob.getPersistentData().putString("apoth.rarity", "apotheosis:" + rarityStr);
    }

    
    private static void socketGems(ItemStack stack, LootRarity bossRarity, RandomSource random) {
        if (bossRarity.ordinal() < 3) return; 

        int sockets = SocketHelper.getSockets(stack);
        if (sockets <= 0) return;

        
        int gemOrdinal = Math.min(bossRarity.ordinal() - 2, RarityRegistry.getMaxRarity().get().ordinal());
        gemOrdinal = Math.max(0, gemOrdinal);
        LootRarity targetGemRarity = RarityRegistry.byOrdinal(gemOrdinal).get();

        
        LootCategory cat = LootCategory.forItem(stack);
        if (cat.isNone()) return;

        
        List<GemInstance> gems = new ArrayList<>(sockets);
        for (int i = 0; i < sockets; i++) gems.add(GemInstance.EMPTY);

        for (int slot = 0; slot < sockets; slot++) {
            
            int currentOrdinal = targetGemRarity.ordinal();
            for (int attempt = 0; attempt < 10 && currentOrdinal >= 0; attempt++) {
                LootRarity rarity = RarityRegistry.byOrdinal(currentOrdinal).get();
                final int minOrdinal = currentOrdinal;
                try {
                    Gem gem = GemRegistry.INSTANCE.getRandomItem(random, 0f, g ->
                        g.getMinRarity().ordinal() <= minOrdinal && g.getMaxRarity().ordinal() >= minOrdinal
                    );
                    if (gem == null) {
                        
                        gem = GemRegistry.INSTANCE.getRandomItem(random, 0f);
                    }
                    if (gem == null) {
                        currentOrdinal--;
                        continue;
                    }

                    LootRarity clampedRarity = gem.clamp(rarity);
                    ItemStack gemStack = GemRegistry.createGemStack(gem, clampedRarity);
                    GemInstance inst = GemInstance.socketed(cat, gemStack);
                    if (inst.isValid()) {
                        gems.set(slot, inst);
                        break;
                    }
                    currentOrdinal--;
                } catch (Exception e) {
                    currentOrdinal--;
                }
            }
        }

        SocketHelper.setGems(stack, new SocketedGems(gems));
    }

    
    private static record PoolResult(List<DynamicHolder<? extends Affix>> pool,
                                     DynamicHolder<LootRarity> usedRarity,
                                     int usedOrdinal) {}

    private static PoolResult getPoolWithRarityFallback(ItemStack stack,
                                                        LootRarity affixRarity,
                                                        Set<DynamicHolder<? extends Affix>> selected,
                                                        AffixType type) {
        int startOrd = affixRarity == null ? 0 : affixRarity.ordinal();
        
        for (int ord = startOrd; ord >= 0; ord--) {
            DynamicHolder<LootRarity> holder = RarityRegistry.byOrdinal(ord);
            if (holder == null || !holder.isBound()) continue;
            LootRarity r = holder.get();
            List<DynamicHolder<? extends Affix>> pool = LootController.getAvailableAffixes(stack, r, selected, type);
            if (!pool.isEmpty()) {
                return new PoolResult(pool, holder, ord);
            }
        }
        
        DynamicHolder<LootRarity> empty = RarityRegistry.byOrdinal(Math.max(0, startOrd));
        return new PoolResult(Collections.emptyList(), empty, startOrd);
    }

    
    private static void compensateAffixes(ItemStack stack, LootRarity displayRarity, LootRarity affixRarity, RandomSource rand) {
        int ord = displayRarity == null ? 0 : displayRarity.ordinal();
        final int minStat, minAbility, minSocket;
        if (ord >= 7) { minStat = 7; minAbility = 5; minSocket = 6; }
        else if (ord == 6) { minStat = 6; minAbility = 4; minSocket = 5; }
        else if (ord == 5) { minStat = 5; minAbility = 3; minSocket = 4; }
        else return;

        
        int usedMaxDropStat = -1;
        int usedMaxDropAbility = -1;
        int safety;

        
        safety = 0;
        while (safety++ < 50) {
            var map = AffixHelper.getAffixes(stack);
            int statN = 0, abilityN = 0;
            Set<DynamicHolder<? extends Affix>> selected = new HashSet<>();
            for (var e : map.entrySet()) {
                AffixType t = e.getValue().affix().get().getType();
                if (t == AffixType.STAT) statN++;
                else if (t == AffixType.ABILITY || t == AffixType.POTION) abilityN++;
                selected.add(e.getKey());
            }
            if (statN >= minStat) break;

            
            AffixType[] tryOrder = { AffixType.STAT, AffixType.ABILITY, AffixType.POTION };
            PoolResult best = new PoolResult(Collections.emptyList(),
                    RarityRegistry.INSTANCE.holder(affixRarity), affixRarity == null ? 0 : affixRarity.ordinal());
            AffixType usedType = AffixType.STAT;
            for (AffixType t : tryOrder) {
                PoolResult pr = getPoolWithRarityFallback(stack, affixRarity, selected, t);
                if (!pr.pool.isEmpty()) {
                    best = pr;
                    usedType = t;
                    break;
                }
            }

            if (best.pool().isEmpty()) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] compensateAffixes: ALL rarities/types pool exhausted for STAT " +
                        "at {}/{} (item={}, LootCategory={})", statN, minStat, stack.getItem(), LootCategory.forItem(stack));
                break;
            }
            usedMaxDropStat = Math.max(usedMaxDropStat, (affixRarity == null ? 0 : affixRarity.ordinal()) - best.usedOrdinal());

            Collections.shuffle(best.pool(), new java.util.Random(rand.nextLong()));
            DynamicHolder<? extends Affix> chosen = best.pool().get(0);
            float lvl = Math.max(0.4F, rand.nextFloat()); 
            AffixInstance inst = new AffixInstance(chosen, stack, best.usedRarity(), lvl);
            AffixHelper.applyAffix(stack, inst);
            IcyApotheosis.LOGGER.info("[icy_apotheosis] STAT+1 [{}/{}] type={} affix={} rarity@ord={} level={}",
                    statN + 1, minStat, usedType, chosen.getId(), best.usedOrdinal(), lvl);
        }

        
        safety = 0;
        while (safety++ < 50) {
            var map = AffixHelper.getAffixes(stack);
            int statN = 0, abilityN = 0;
            Set<DynamicHolder<? extends Affix>> selected = new HashSet<>();
            for (var e : map.entrySet()) {
                AffixType t = e.getValue().affix().get().getType();
                if (t == AffixType.STAT) statN++;
                else if (t == AffixType.ABILITY || t == AffixType.POTION) abilityN++;
                selected.add(e.getKey());
            }
            if (abilityN >= minAbility) break;

            AffixType[] tryOrder = { AffixType.ABILITY, AffixType.STAT, AffixType.POTION };
            PoolResult best = new PoolResult(Collections.emptyList(),
                    RarityRegistry.INSTANCE.holder(affixRarity), affixRarity == null ? 0 : affixRarity.ordinal());
            AffixType usedType = AffixType.ABILITY;
            for (AffixType t : tryOrder) {
                PoolResult pr = getPoolWithRarityFallback(stack, affixRarity, selected, t);
                if (!pr.pool.isEmpty()) {
                    best = pr;
                    usedType = t;
                    break;
                }
            }

            if (best.pool().isEmpty()) {
                IcyApotheosis.LOGGER.error("[icy_apotheosis] compensateAffixes: ALL rarities/types pool exhausted for ABILITY " +
                        "at {}/{} (item={}, LootCategory={})", abilityN, minAbility, stack.getItem(), LootCategory.forItem(stack));
                break;
            }
            usedMaxDropAbility = Math.max(usedMaxDropAbility, (affixRarity == null ? 0 : affixRarity.ordinal()) - best.usedOrdinal());

            Collections.shuffle(best.pool(), new java.util.Random(rand.nextLong()));
            DynamicHolder<? extends Affix> chosen = best.pool().get(0);
            float lvl = Math.max(0.4F, rand.nextFloat());
            AffixInstance inst = new AffixInstance(chosen, stack, best.usedRarity(), lvl);
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
            AffixType t = e.affix().get().getType();
            if (t == AffixType.STAT) fStat++;
            else if (t == AffixType.ABILITY || t == AffixType.POTION) fAb++;
        }
        int fSock = SocketHelper.getSockets(stack);
        IcyApotheosis.LOGGER.info("[icy_apotheosis] ============================================================");
        IcyApotheosis.LOGGER.info("[icy_apotheosis] compensateAffixes DONE: display={} STAT={}/{} ABILITY={}/{} SOCKET={}/{} item={}",
                displayRarity, fStat, minStat, fAb, minAbility, fSock, minSocket, stack.getItem());
        if (usedMaxDropStat > 0 || usedMaxDropAbility > 0) {
            IcyApotheosis.LOGGER.info("[icy_apotheosis]   → max rarity drop used: STAT={} ABILITY={} (from affixRarity ord={} down)",
                    usedMaxDropStat, usedMaxDropAbility, affixRarity == null ? "?" : affixRarity.ordinal());
        }
        IcyApotheosis.LOGGER.info("[icy_apotheosis] ============================================================");
    }

    private static void modifyBossItem(ItemStack stack, String bossName, LootRarity rarity, RandomSource random) {
        try {
            int enchantLvl = Math.max(10, rarity.ordinal() * 10 + 10);
            ApothBoss.enchantBossItem(random, stack, enchantLvl, true);

            String ownershipFormat = NameHelper.ownershipFormat;
            String bossOwnerName = String.format(ownershipFormat, bossName);

            MutableComponent itemName = stack.getHoverName().copy();
            MutableComponent newName = Component.translatable("misc.apotheosis.affix_name.four",
                bossOwnerName, itemName);

            AffixHelper.setName(stack, newName);

            stack.getOrCreateTag().putBoolean("apoth_boss", true);
        } catch (Exception e) {
            IcyApotheosis.LOGGER.warn("[icy_apotheosis] Error modifying boss item: {}", e.getMessage());
            stack.getOrCreateTag().putBoolean("apoth_boss", true);
        }
    }
}
