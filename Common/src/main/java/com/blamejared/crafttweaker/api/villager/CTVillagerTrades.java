package com.blamejared.crafttweaker.api.villager;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.villager.ActionAddTrade;
import com.blamejared.crafttweaker.api.action.villager.ActionAddWanderingTrade;
import com.blamejared.crafttweaker.api.action.villager.ActionRemoveTrade;
import com.blamejared.crafttweaker.api.action.villager.ActionRemoveWanderingTrade;
import com.blamejared.crafttweaker.api.action.villager.ActionTradeBase;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.mixin.common.access.villager.AccessDyedArmorForEmeralds;
import com.blamejared.crafttweaker.mixin.common.access.villager.AccessEmeraldForItems;
import com.blamejared.crafttweaker.mixin.common.access.villager.AccessEnchantedItemForEmeralds;
import com.blamejared.crafttweaker.mixin.common.access.villager.AccessItemsAndEmeraldsToItems;
import com.blamejared.crafttweaker.mixin.common.access.villager.AccessItemsForEmeralds;
import com.blamejared.crafttweaker.mixin.common.access.villager.AccessTippedArrowForItemsAndEmeralds;
import com.blamejared.crafttweaker.platform.Services;
import com.blamejared.crafttweaker_annotations.annotations.Document;
import net.minecraft.Util;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.openzen.zencode.java.ZenCodeGlobals;
import org.openzen.zencode.java.ZenCodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @docParam this villagerTrades
 */
@ZenRegister
@ZenCodeType.Name("crafttweaker.api.villagers.VillagerTrades")
@Document("vanilla/api/villager/VillagerTrades")
public class CTVillagerTrades {
    
    @ZenCodeGlobals.Global("villagerTrades")
    public static final CTVillagerTrades INSTANCE = new CTVillagerTrades();
    public static final List<ActionTradeBase> ACTIONS_VILLAGER_TRADES = new ArrayList<>();
    public static final List<ActionTradeBase> ACTION_WANDERING_TRADES = new ArrayList<>();
    // The event only fires once, so we use this to make sure we don't constantly add to the above lists
    public static boolean RAN_EVENTS = false;
    
    // If you are a modder looking at this map and wondering how you can add your own trade type here,
    // make a GitHub issue! We are super open to making a whole system to register
    // your custom trade types but don't want to waste time if there is no interest.
    // The system we add would be similar to IRecipeHandler, with an annotation based loading system.
    public static final Map<Class<? extends VillagerTrades.ItemListing>, Function<VillagerTrades.ItemListing, CTTradeObject>> TRADE_CONVERTER = Util.make(new HashMap<>(), classFunctionMap -> {
        final IItemStack emerald = Services.PLATFORM.createMCItemStackMutable(new ItemStack(Items.EMERALD));
        final IItemStack compass = Services.PLATFORM.createMCItemStackMutable(new ItemStack(Items.COMPASS));
        final IItemStack book = Services.PLATFORM.createMCItemStackMutable(new ItemStack(Items.BOOK));
        final IItemStack enchantedBook = Services.PLATFORM.createMCItemStackMutable(new ItemStack(Items.ENCHANTED_BOOK));
        final IItemStack filledMap = Services.PLATFORM.createMCItemStackMutable(new ItemStack(Items.FILLED_MAP));
        final IItemStack suspiciousStew = Services.PLATFORM.createMCItemStackMutable(new ItemStack(Items.SUSPICIOUS_STEW));
        
        classFunctionMap.put(VillagerTrades.DyedArmorForEmeralds.class, iTrade -> {
            if(iTrade instanceof VillagerTrades.DyedArmorForEmeralds) {
                return new CTTradeObject(
                        emerald,
                        Services.PLATFORM.getEmptyIItemStack(),
                        Services.PLATFORM.createMCItemStackMutable(((AccessDyedArmorForEmeralds) iTrade).getItem()
                                .getDefaultInstance()));
            }
            throw new IllegalArgumentException("Invalid trade passed to trade function! Given: " + iTrade.getClass());
        });
        classFunctionMap.put(VillagerTrades.EmeraldForItems.class, iTrade -> {
            if(iTrade instanceof VillagerTrades.EmeraldForItems) {
                return new CTTradeObject(
                        Services.PLATFORM.createMCItemStackMutable(((AccessEmeraldForItems) iTrade).getItem()
                                .getDefaultInstance()),
                        Services.PLATFORM.getEmptyIItemStack(),
                        emerald);
            }
            throw new IllegalArgumentException("Invalid trade passed to trade function! Given: " + iTrade.getClass());
        });
        classFunctionMap.put(VillagerTrades.TreasureMapForEmeralds.class, iTrade -> new CTTradeObject(
                emerald,
                compass,
                filledMap));
        classFunctionMap.put(VillagerTrades.EmeraldsForVillagerTypeItem.class, iTrade -> new CTTradeObject(
                // This trade has random inputs, there isn't a good way to get them, so just going to use air.
                Services.PLATFORM.getEmptyIItemStack(),
                Services.PLATFORM.getEmptyIItemStack(),
                emerald));
        classFunctionMap.put(VillagerTrades.EnchantBookForEmeralds.class, iTrade -> new CTTradeObject(
                emerald,
                book,
                enchantedBook));
        classFunctionMap.put(VillagerTrades.EnchantedItemForEmeralds.class, iTrade -> {
            if(iTrade instanceof VillagerTrades.EnchantedItemForEmeralds) {
                return new CTTradeObject(
                        emerald,
                        Services.PLATFORM.getEmptyIItemStack(),
                        Services.PLATFORM.createMCItemStackMutable(((AccessEnchantedItemForEmeralds) iTrade).getItemStack()));
            }
            throw new IllegalArgumentException("Invalid trade passed to trade function! Given: " + iTrade.getClass());
        });
        classFunctionMap.put(VillagerTrades.TippedArrowForItemsAndEmeralds.class, iTrade -> {
            if(iTrade instanceof VillagerTrades.TippedArrowForItemsAndEmeralds) {
                return new CTTradeObject(
                        emerald,
                        Services.PLATFORM.createMCItemStackMutable(((AccessTippedArrowForItemsAndEmeralds) iTrade).getFromItem()
                                .getDefaultInstance()),
                        Services.PLATFORM.createMCItemStackMutable(((AccessTippedArrowForItemsAndEmeralds) iTrade).getToItem()));
            }
            throw new IllegalArgumentException("Invalid trade passed to trade function! Given: " + iTrade.getClass());
        });
        classFunctionMap.put(VillagerTrades.ItemsAndEmeraldsToItems.class, iTrade -> {
            if(iTrade instanceof VillagerTrades.ItemsAndEmeraldsToItems) {
                return new CTTradeObject(
                        emerald,
                        Services.PLATFORM.createMCItemStackMutable(((AccessItemsAndEmeraldsToItems) iTrade).getFromItem()),
                        Services.PLATFORM.createMCItemStackMutable(((AccessItemsAndEmeraldsToItems) iTrade).getToItem()));
            }
            throw new IllegalArgumentException("Invalid trade passed to trade function! Given: " + iTrade.getClass());
        });
        classFunctionMap.put(VillagerTrades.ItemsForEmeralds.class, iTrade -> {
            if(iTrade instanceof VillagerTrades.ItemsForEmeralds) {
                return new CTTradeObject(
                        emerald,
                        Services.PLATFORM.getEmptyIItemStack(),
                        Services.PLATFORM.createMCItemStackMutable(((AccessItemsForEmeralds) iTrade).getItemStack()));
            }
            throw new IllegalArgumentException("Invalid trade passed to trade function! Given: " + iTrade.getClass());
        });
        classFunctionMap.put(VillagerTrades.SuspiciousStewForEmerald.class, iTrade -> new CTTradeObject(
                emerald,
                Services.PLATFORM.getEmptyIItemStack(),
                suspiciousStew));
    });
    
    /**
     * Adds a Villager Trade for emeralds for an Item. An example being, giving a villager 2 emeralds for an arrow.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param emeralds      The amount of Emeralds.
     * @param forSale       What ItemStack is being sold (by the Villager).
     * @param maxTrades     How many times can this trade be done.
     * @param xp            How much Experience is given by trading.
     * @param priceMult     When this trade is discounted, how much should it be discounted by.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam emeralds 16
     * @docParam forSale <item:minecraft:diamond>
     * @docParam maxTrades 5
     * @docParam xp 2
     * @docParam priceMult 0.05
     */
    @ZenCodeType.Method
    public void addTrade(VillagerProfession profession, int villagerLevel, int emeralds, ItemStack forSale, int maxTrades, int xp, @ZenCodeType.OptionalFloat(1.0f) float priceMult) {
        
        BasicTradeListing trade = new BasicTradeListing(emeralds, forSale, maxTrades, xp, priceMult);
        addTradeInternal(profession, villagerLevel, trade);
    }
    
    /**
     * Adds a Villager Trade for an Item for an Item. An example being, giving a villager 2 diamonds for an arrow.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param input1        The ItemStack that is being given to the Villager.
     * @param forSale       What ItemStack is being sold (by the Villager).
     * @param maxTrades     How many times can this trade be done.
     * @param xp            How much Experience is given by trading.
     * @param priceMult     When this trade is discounted, how much should it be discounted by.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam input1 <item:minecraft:dirt> * 16
     * @docParam forSale <item:minecraft:diamond>
     * @docParam maxTrades 5
     * @docParam xp 2
     * @docParam priceMult 0.05
     */
    @ZenCodeType.Method
    public void addTrade(VillagerProfession profession, int villagerLevel, ItemStack input1, ItemStack forSale, int maxTrades, int xp, @ZenCodeType.OptionalFloat(1.0f) float priceMult) {
        
        BasicTradeListing trade = new BasicTradeListing(input1, forSale, maxTrades, xp, priceMult);
        addTradeInternal(profession, villagerLevel, trade);
    }
    
    /**
     * Adds a Villager Trade for two Items for an Item. An example being, giving a villager 2 diamonds and 2 dirt for an arrow.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param input1        The main ItemStack that is being given to the Villager.
     * @param input2        The secondary ItemStack that is being given to the Villager.
     * @param forSale       What ItemStack is being sold (by the Villager).
     * @param maxTrades     How many times can this trade be done.
     * @param xp            How much Experience is given by trading.
     * @param priceMult     When this trade is discounted, how much should it be discounted by.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam input1 <item:minecraft:diamond> * 2
     * @docParam input2 <item:minecraft:dirt> * 2
     * @docParam forSale <item:minecraft:arrow>
     * @docParam maxTrades 5
     * @docParam xp 2
     * @docParam priceMult 0.05
     */
    @ZenCodeType.Method
    public void addTrade(VillagerProfession profession, int villagerLevel, ItemStack input1, ItemStack input2, ItemStack forSale, int maxTrades, int xp, @ZenCodeType.OptionalFloat(1.0f) float priceMult) {
        
        BasicTradeListing trade = new BasicTradeListing(input1, input2, forSale, maxTrades, xp, priceMult);
        addTradeInternal(profession, villagerLevel, trade);
    }
    
    /**
     * Removes a `BasicTrade` Villager trade. `BasicTrades` are trades that allow any item, to any other item. It is only really used for mod recipes and is not used for any vanilla villager trade.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param forSale       What ItemStack is being sold (by the Villager).
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam forSale <item:minecraft:arrow>
     * @docParam price <item:minecraft:stick>
     * @docParam price2 <item:minecraft:emerald>
     */
    @ZenCodeType.Method
    public void removeBasicTrade(VillagerProfession profession, int villagerLevel, IItemStack forSale, @ZenCodeType.Optional("<item:minecraft:air>") IItemStack price, @ZenCodeType.Optional("<item:minecraft:air>") IItemStack price2) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof IBasicItemListing basicTrade) {
                boolean saleMatches = forSale.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getForSale()));
                if(price.isEmpty() && price2.isEmpty()) {
                    return saleMatches;
                }
                boolean priceMatches = price.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getPrice()));
                if(!price.isEmpty() && price2.isEmpty()) {
                    return saleMatches && priceMatches;
                }
                boolean price2Matches = price2.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getPrice2()));
                return saleMatches && priceMatches && price2Matches;
            }
            return false;
        });
    }
    
    /**
     * Removes a Villager trade for Emeralds for Items. An example being, giving a villager 20 Wheat and getting an Emerald from the villager.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param tradeFor      What ItemStack is being sold (by the Villager).
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam tradeFor <item:minecraft:potato>.definition
     */
    @ZenCodeType.Method
    public void removeEmeraldForItemsTrade(VillagerProfession profession, int villagerLevel, Item tradeFor) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof VillagerTrades.EmeraldForItems) {
                return ((AccessEmeraldForItems) trade).getItem() == tradeFor;
            } else if(trade instanceof IBasicItemListing basicTrade) {
                return basicTrade.getForSale().getItem() == tradeFor;
            }
            return false;
        });
    }
    
    /**
     * Removes a Villager trade for Items for Emeralds. An example being, giving a villager an Emerald and getting 4 Pumpkin Pies from the villager.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param sellingItem   What ItemStack is being given to the Villager.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam sellingItem <item:minecraft:apple>
     */
    @ZenCodeType.Method
    public void removeItemsForEmeraldsTrade(VillagerProfession profession, int villagerLevel, IItemStack sellingItem) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof VillagerTrades.ItemsForEmeralds) {
                return sellingItem.matches(Services.PLATFORM.createMCItemStackMutable(((AccessItemsForEmeralds) trade).getItemStack()));
            } else if(trade instanceof IBasicItemListing basicTrade) {
                return Services.PLATFORM.createMCItemStackMutable(basicTrade.getPrice())
                        .matches(sellingItem);
            }
            return false;
        });
    }
    
    /**
     * Removes a Villager trade for Emeralds and Items for Items. An example being, giving a villager 6 uncooked Cod and an Emerald and getting back 6 Cooked Cod.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param sellingItem   What ItemStack is being given to the Villager.
     * @param buyingItem    The item that the Villager is selling.
     *
     * @docParam profession <profession:minecraft:fisherman>
     * @docParam villagerLevel 1
     * @docParam sellingItem <item:minecraft:cooked_cod>
     * @docParam buyingItem <item:minecraft:cod>
     */
    @ZenCodeType.Method
    public void removeItemsAndEmeraldsToItemsTrade(VillagerProfession profession, int villagerLevel, IItemStack sellingItem, IItemStack buyingItem) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof VillagerTrades.ItemsAndEmeraldsToItems) {
                if(sellingItem.matches(Services.PLATFORM.createMCItemStackMutable(((AccessItemsAndEmeraldsToItems) trade).getToItem()))) {
                    return buyingItem.matches(Services.PLATFORM.createMCItemStackMutable(((AccessItemsAndEmeraldsToItems) trade).getFromItem()));
                }
            } else if(trade instanceof IBasicItemListing basicTrade) {
                if(sellingItem.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getPrice()))) {
                    return buyingItem.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getForSale()));
                }
            }
            return false;
        });
    }
    
    /**
     * Removes a Villager trade for Items for an Item with a PotionEffect. An example being, giving a villager an Arrow and an Emerald and getting a Tipped Arrow with night vision.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param potionStack   The base ItemStack that a random potion effect will be applied to. E.G. A tipped Arrow with no effect applied.
     * @param sellingItem   What ItemStack is being given to the Villager.
     *
     * @docParam profession <profession:minecraft:fletcher>
     * @docParam villagerLevel 1
     * @docParam potionStack <item:minecraft:tipped_arrow>
     * @docParam sellingItem <item:minecraft:arrow>
     */
    @ZenCodeType.Method
    public void removeTippedArrowForItemsAndEmeraldsTrade(VillagerProfession profession, int villagerLevel, IItemStack potionStack, Item sellingItem) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof VillagerTrades.TippedArrowForItemsAndEmeralds) {
                if(potionStack.matches(Services.PLATFORM.createMCItemStackMutable(((AccessTippedArrowForItemsAndEmeralds) trade).getToItem()))) {
                    return sellingItem == ((AccessTippedArrowForItemsAndEmeralds) trade).getFromItem();
                }
            }
            return false;
        });
    }
    
    /**
     * Removes a Villager trade for Items for Dyed leather armor. An example being, giving a villager Leather Leggings and 3 Emeralds and getting a Blue Dyed Leather Leggings.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param buyingItem    The base ItemStack that a random Dye colour will be applied to. E.G. A leather chestplate with no effect applied.
     *
     * @docParam profession <profession:minecraft:leatherworker>
     * @docParam villagerLevel 1
     * @docParam buyingItem <item:minecraft:leather_chestplate>
     */
    @ZenCodeType.Method
    public void removeDyedArmorForEmeraldsTrade(VillagerProfession profession, int villagerLevel, Item buyingItem) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof VillagerTrades.DyedArmorForEmeralds) {
                return ((AccessDyedArmorForEmeralds) trade).getItem() == buyingItem;
            }
            return false;
        });
    }
    
    /**
     * Removes a Villager trade for a Map. An example being, giving a villager 13 Emeralds and getting a Map to a structure.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     *
     * @docParam profession <profession:minecraft:cartographer>
     * @docParam villagerLevel 1
     */
    @ZenCodeType.Method
    public void removeTreasureMapForEmeraldsTrade(VillagerProfession profession, int villagerLevel) {
        
        removeTradeInternal(profession, villagerLevel, trade -> trade instanceof VillagerTrades.TreasureMapForEmeralds);
    }
    
    /**
     * Removes a Villager trade for an Enchanted Book. An example being, giving a villager Emeralds and getting an Enchanted Book with a random Enchantment.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     *
     * @docParam profession <profession:minecraft:librarian>
     * @docParam villagerLevel 1
     */
    @ZenCodeType.Method
    public void removeEnchantBookForEmeraldsTrade(VillagerProfession profession, int villagerLevel) {
        
        removeTradeInternal(profession, villagerLevel, trade -> trade instanceof VillagerTrades.EnchantBookForEmeralds);
    }
    
    /**
     * Removes a Villager trade for an Enchanted Item. An example being, giving a villager 3 Emeralds and getting an Enchanted Pickaxe.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     * @param buyingItem    The ItemStack that the Villager is selling (including any NBT).
     *
     * @docParam profession <profession:minecraft:armorer>
     * @docParam villagerLevel 1
     * @docParam buyingItem <item:minecraft:diamond_boots>
     */
    @ZenCodeType.Method
    public void removeEnchantedItemForEmeraldsTrade(VillagerProfession profession, int villagerLevel, IItemStack buyingItem) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            if(trade instanceof VillagerTrades.EnchantedItemForEmeralds) {
                return buyingItem.matches(Services.PLATFORM.createMCItemStackMutable(((AccessEnchantedItemForEmeralds) trade).getItemStack()));
            }
            return false;
        });
        
    }
    
    /**
     * Removes a Villager trade for Suspicious Stew. An example being, giving a villager an Emerald and getting a bowl of Suspicious Stew back.
     *
     * @param profession    What profession this trade should be for.
     * @param villagerLevel The level the Villager needs to be.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     */
    @ZenCodeType.Method
    public void removeSuspiciousStewForEmeraldTrade(VillagerProfession profession, int villagerLevel) {
        
        removeTradeInternal(profession, villagerLevel, trade -> trade instanceof VillagerTrades.SuspiciousStewForEmerald);
    }
    
    /**
     * Removes all the trades for the given profession and villagerLevel
     *
     * @param profession    hat profession to remove from.
     * @param villagerLevel The level the Villager needs to be.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     */
    @ZenCodeType.Method
    public void removeAllTrades(VillagerProfession profession, int villagerLevel) {
        
        removeTradeInternal(profession, villagerLevel, trade -> true);
    }
    
    /**
     * Removes the specified trade for the given profession and villagerLevel.
     *
     * @param profession    That profession to remove from.
     * @param villagerLevel The level the Villager needs to be.
     * @param buying        The first item that you are giving to the villager.
     * @param selling       The item that the villager is selling to you.
     * @param secondBuying  The second item that you are giving to the villager. Will default to air if not provided.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam buying <item:minecraft:potato>
     * @docParam selling <item:minecraft:emerald>
     * @docParam secondBuying <item:minecraft:air>
     */
    @ZenCodeType.Method
    public void removeTrade(VillagerProfession profession, int villagerLevel, IIngredient buying, IIngredient selling, @ZenCodeType.Optional("<item:minecraft:air>") IIngredient secondBuying) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            Function<VillagerTrades.ItemListing, CTTradeObject> tradeFunc = TRADE_CONVERTER.get(trade.getClass());
            if(tradeFunc == null) {
                return false;
            }
            CTTradeObject tradeObject = tradeFunc.apply(trade);
            if(!buying.matches(tradeObject.getBuyingStack())) {
                return false;
            }
            if(!selling.matches(tradeObject.getSellingStack())) {
                return false;
            }
            return secondBuying.matches(tradeObject.getBuyingStackSecond());
        });
    }
    
    /**
     * Removes all trades that sell the specified item for the given profession and villagerLevel.
     *
     * @param profession    That profession to remove from.
     * @param villagerLevel The level the Villager needs to be.
     * @param selling       The item that the villager is selling to you.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam selling <item:minecraft:emerald>
     */
    @ZenCodeType.Method
    public void removeTradesSelling(VillagerProfession profession, int villagerLevel, IIngredient selling) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            Function<VillagerTrades.ItemListing, CTTradeObject> tradeFunc = TRADE_CONVERTER.get(trade.getClass());
            if(tradeFunc == null) {
                return false;
            }
            CTTradeObject tradeObject = tradeFunc.apply(trade);
            return selling.matches(tradeObject.getSellingStack());
        });
    }
    
    /**
     * Removes all trades that have the specified item as the buying item for the given profession and villagerLevel.
     *
     * @param profession    That profession to remove from.
     * @param villagerLevel The level the Villager needs to be.
     * @param buying        The first item that you are giving to the villager.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam buying <item:minecraft:potato>
     */
    @ZenCodeType.Method
    public void removeTradesBuying(VillagerProfession profession, int villagerLevel, IIngredient buying) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            Function<VillagerTrades.ItemListing, CTTradeObject> tradeFunc = TRADE_CONVERTER.get(trade.getClass());
            if(tradeFunc == null) {
                return false;
            }
            CTTradeObject tradeObject = tradeFunc.apply(trade);
            return buying.matches(tradeObject.getBuyingStack());
        });
    }
    
    /**
     * Removes all trades that have the specified items as the buying items for the given profession and villagerLevel.
     *
     * @param profession    That profession to remove from.
     * @param villagerLevel The level the Villager needs to be.
     * @param buying        The first item that you are giving to the villager.
     * @param secondBuying  The second item that you are giving to the villager. Will default to air if not provided.
     *
     * @docParam profession <profession:minecraft:farmer>
     * @docParam villagerLevel 1
     * @docParam buying <item:minecraft:potato>
     * @docParam secondBuying <item:minecraft:air>
     */
    @ZenCodeType.Method
    public void removeTradesBuying(VillagerProfession profession, int villagerLevel, IIngredient buying, IIngredient secondBuying) {
        
        removeTradeInternal(profession, villagerLevel, trade -> {
            Function<VillagerTrades.ItemListing, CTTradeObject> tradeFunc = TRADE_CONVERTER.get(trade.getClass());
            if(tradeFunc == null) {
                return false;
            }
            CTTradeObject tradeObject = tradeFunc.apply(trade);
            if(!buying.matches(tradeObject.getBuyingStack())) {
                return false;
            }
            return secondBuying.matches(tradeObject.getBuyingStackSecond());
        });
    }
    
    
    /**
     * Adds a Wandering Trader Trade for emeralds for an Item. An example being, giving a Wandering Trader 2 emeralds for an arrow.
     *
     * @param rarity    The rarity of the Trade. Valid options are `1` or `2`. A Wandering Trader can only spawn with a single trade of rarity `2`.
     * @param emeralds  The amount of Emeralds.
     * @param forSale   What ItemStack is being sold (by the Wandering Trader).
     * @param maxTrades How many times can this trade be done.
     * @param xp        How much Experience is given by trading.
     *
     * @docParam rarity 1
     * @docParam emeralds 16
     * @docParam forSale <item:minecraft:diamond>
     * @docParam maxTrades 16
     * @docParam xp 2
     */
    @ZenCodeType.Method
    public void addWanderingTrade(int rarity, int emeralds, ItemStack forSale, int maxTrades, int xp) {
        
        BasicTradeListing trade = new BasicTradeListing(emeralds, forSale, maxTrades, xp, 1);
        addWanderingTradeInternal(rarity, trade);
    }
    
    /**
     * Adds a Wandering Trader Trade for emeralds for an Item. An example being, giving a Wandering Trader 2 emeralds for an arrow.
     *
     * @param rarity    The rarity of the Trade. Valid options are `1` or `2`. A Wandering Trader can only spawn with a single trade of rarity `2`.
     * @param price     The ItemStack being given to the Wandering Trader.
     * @param forSale   What ItemStack is being sold (by the Wandering Trader).
     * @param maxTrades How many times can this trade be done.
     * @param xp        How much Experience is given by trading.
     *
     * @docParam rarity 1
     * @docParam price <item:minecraft:dirt>
     * @docParam forSale <item:minecraft:diamond>
     * @docParam maxTrades 16
     * @docParam xp 2
     */
    @ZenCodeType.Method
    public void addWanderingTrade(int rarity, IItemStack price, IItemStack forSale, int maxTrades, int xp) {
        
        BasicTradeListing trade = new BasicTradeListing(price.getInternal(), forSale.getInternal(), maxTrades, xp, 1);
        addWanderingTradeInternal(rarity, trade);
    }
    
    /**
     * Removes a Wandering Trader trade for Emeralds for Items. An example being, giving a Wandering Trader 2 Emeralds for an Arrow.
     *
     * @param rarity   The rarity of the Trade. Valid options are `1` or `2`. A Wandering Trader can only spawn with a single trade of rarity `2`.
     * @param tradeFor What ItemStack is being sold (by the Villager).
     *
     * @docParam rarity 2
     * @docParam tradeFor <item:minecraft:arrow>
     */
    @ZenCodeType.Method
    public void removeWanderingTrade(int rarity, IItemStack tradeFor) {
        
        removeWanderingTradeInternal(rarity, trade -> {
            if(trade instanceof VillagerTrades.ItemsForEmeralds) {
                return tradeFor.matches(Services.PLATFORM.createMCItemStackMutable(((AccessItemsForEmeralds) trade).getItemStack()));
            } else if(trade instanceof IBasicItemListing basicTrade) {
                return tradeFor.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getForSale()));
            }
            return false;
        });
    }
    
    /**
     * Removes a Wandering Trader trade for Emeralds for Items. An example being, giving a Wandering Trader 2 Emeralds for an Arrow.
     *
     * @param rarity   The rarity of the Trade. Valid options are `1` or `2`. A Wandering Trader can only spawn with a single trade of rarity `2`.
     * @param tradeFor What ItemStack is being sold (by the Villager).
     *
     * @docParam rarity 2
     * @docParam tradeFor <item:minecraft:arrow>
     */
    @ZenCodeType.Method
    public void removeWanderingTrade(int rarity, IIngredient tradeFor) {
        
        removeWanderingTradeInternal(rarity, trade -> {
            if(trade instanceof VillagerTrades.ItemsForEmeralds) {
                return tradeFor.matches(Services.PLATFORM.createMCItemStackMutable(((AccessItemsForEmeralds) trade).getItemStack()));
            } else if(trade instanceof IBasicItemListing basicTrade) {
                return tradeFor.matches(Services.PLATFORM.createMCItemStackMutable(basicTrade.getForSale()));
            }
            return false;
        });
    }
    
    /**
     * Removes all wandering trades of the given rarity
     *
     * @param rarity The rarity of the Trade. Valid options are `1` or `2`. A Wandering Trader can only spawn with a single trade of rarity `2`.
     */
    @ZenCodeType.Method
    public void removeAllWanderingTrades(int rarity) {
        
        removeWanderingTradeInternal(rarity, trade -> true);
    }
    
    private void addTradeInternal(VillagerProfession profession, int villagerLevel, VillagerTrades.ItemListing trade) {
        
        apply(new ActionAddTrade(profession, villagerLevel, trade), false);
    }
    
    private void addWanderingTradeInternal(int villagerLevel, VillagerTrades.ItemListing trade) {
        
        apply(new ActionAddWanderingTrade(villagerLevel, trade), true);
    }
    
    private void removeTradeInternal(VillagerProfession profession, int villagerLevel, ITradeRemover remover) {
        
        apply(new ActionRemoveTrade(profession, villagerLevel, remover), false);
    }
    
    private void removeWanderingTradeInternal(int villagerLevel, ITradeRemover remover) {
        
        apply(new ActionRemoveWanderingTrade(villagerLevel, remover), true);
    }
    
    private void apply(ActionTradeBase action, boolean wandering) {
        
        if(!CTVillagerTrades.RAN_EVENTS) {
            if(wandering) {
                CTVillagerTrades.ACTION_WANDERING_TRADES.add(action);
            } else {
                CTVillagerTrades.ACTIONS_VILLAGER_TRADES.add(action);
            }
        }
        CraftTweakerAPI.apply(action);
    }
    
    
}