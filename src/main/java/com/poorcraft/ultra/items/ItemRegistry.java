package com.poorcraft.ultra.items;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Central registry for all item types (singleton pattern).
 * CP v3.1: Item System
 */
public class ItemRegistry {
    private static final Logger LOGGER = Logger.getLogger(ItemRegistry.class.getName());
    private static ItemRegistry instance;
    
    private final Map<Integer, ItemDefinition> items = new HashMap<>();
    private final Map<String, ItemDefinition> itemsByName = new HashMap<>();
    
    public static final ItemDefinition AIR = ItemDefinition.builder()
            .id(0)
            .name("air")
            .displayName("Air")
            .icon("air.png")
            .maxStackSize(0)
            .build();
    
    private ItemRegistry() {
        init();
    }
    
    public static ItemRegistry getInstance() {
        if (instance == null) {
            instance = new ItemRegistry();
        }
        return instance;
    }
    
    private void init() {
        LOGGER.info("Initializing ItemRegistry...");
        
        // Register AIR
        register(AIR);
        
        // Tools (IDs 1000-1024)
        registerTools();
        
        // Materials (IDs 1025-1049)
        registerMaterials();
        
        // Food (IDs 1050-1059)
        registerFood();
        
        // Block items (IDs 1060-1099)
        registerBlockItems();
        
        LOGGER.info("ItemRegistry initialized: " + items.size() + " items");
    }
    
    private void registerTools() {
        // Wooden tools
        register(ItemDefinition.builder().id(1000).name("wooden_pickaxe").displayName("Wooden Pickaxe")
                .icon("wooden_pickaxe.png").maxStackSize(1).durability(59)
                .toolType(ToolType.PICKAXE).toolTier(ToolTier.WOOD).build());
        register(ItemDefinition.builder().id(1001).name("wooden_axe").displayName("Wooden Axe")
                .icon("wooden_axe.png").maxStackSize(1).durability(59)
                .toolType(ToolType.AXE).toolTier(ToolTier.WOOD).build());
        register(ItemDefinition.builder().id(1002).name("wooden_shovel").displayName("Wooden Shovel")
                .icon("wooden_shovel.png").maxStackSize(1).durability(59)
                .toolType(ToolType.SHOVEL).toolTier(ToolTier.WOOD).build());
        register(ItemDefinition.builder().id(1003).name("wooden_hoe").displayName("Wooden Hoe")
                .icon("wooden_hoe.png").maxStackSize(1).durability(59)
                .toolType(ToolType.HOE).toolTier(ToolTier.WOOD).build());
        register(ItemDefinition.builder().id(1004).name("wooden_sword").displayName("Wooden Sword")
                .icon("wooden_sword.png").maxStackSize(1).durability(59)
                .toolType(ToolType.SWORD).toolTier(ToolTier.WOOD).build());
        
        // Stone tools
        register(ItemDefinition.builder().id(1005).name("stone_pickaxe").displayName("Stone Pickaxe")
                .icon("stone_pickaxe.png").maxStackSize(1).durability(131)
                .toolType(ToolType.PICKAXE).toolTier(ToolTier.STONE).build());
        register(ItemDefinition.builder().id(1006).name("stone_axe").displayName("Stone Axe")
                .icon("stone_axe.png").maxStackSize(1).durability(131)
                .toolType(ToolType.AXE).toolTier(ToolTier.STONE).build());
        register(ItemDefinition.builder().id(1007).name("stone_shovel").displayName("Stone Shovel")
                .icon("stone_shovel.png").maxStackSize(1).durability(131)
                .toolType(ToolType.SHOVEL).toolTier(ToolTier.STONE).build());
        register(ItemDefinition.builder().id(1008).name("stone_hoe").displayName("Stone Hoe")
                .icon("stone_hoe.png").maxStackSize(1).durability(131)
                .toolType(ToolType.HOE).toolTier(ToolTier.STONE).build());
        register(ItemDefinition.builder().id(1009).name("stone_sword").displayName("Stone Sword")
                .icon("stone_sword.png").maxStackSize(1).durability(131)
                .toolType(ToolType.SWORD).toolTier(ToolTier.STONE).build());
        
        // Iron tools
        register(ItemDefinition.builder().id(1010).name("iron_pickaxe").displayName("Iron Pickaxe")
                .icon("iron_pickaxe.png").maxStackSize(1).durability(250)
                .toolType(ToolType.PICKAXE).toolTier(ToolTier.IRON).build());
        register(ItemDefinition.builder().id(1011).name("iron_axe").displayName("Iron Axe")
                .icon("iron_axe.png").maxStackSize(1).durability(250)
                .toolType(ToolType.AXE).toolTier(ToolTier.IRON).build());
        register(ItemDefinition.builder().id(1012).name("iron_shovel").displayName("Iron Shovel")
                .icon("iron_shovel.png").maxStackSize(1).durability(250)
                .toolType(ToolType.SHOVEL).toolTier(ToolTier.IRON).build());
        register(ItemDefinition.builder().id(1013).name("iron_hoe").displayName("Iron Hoe")
                .icon("iron_hoe.png").maxStackSize(1).durability(250)
                .toolType(ToolType.HOE).toolTier(ToolTier.IRON).build());
        register(ItemDefinition.builder().id(1014).name("iron_sword").displayName("Iron Sword")
                .icon("iron_sword.png").maxStackSize(1).durability(250)
                .toolType(ToolType.SWORD).toolTier(ToolTier.IRON).build());
        
        // Gold tools
        register(ItemDefinition.builder().id(1015).name("gold_pickaxe").displayName("Gold Pickaxe")
                .icon("gold_pickaxe.png").maxStackSize(1).durability(32)
                .toolType(ToolType.PICKAXE).toolTier(ToolTier.GOLD).build());
        register(ItemDefinition.builder().id(1016).name("gold_axe").displayName("Gold Axe")
                .icon("gold_axe.png").maxStackSize(1).durability(32)
                .toolType(ToolType.AXE).toolTier(ToolTier.GOLD).build());
        register(ItemDefinition.builder().id(1017).name("gold_shovel").displayName("Gold Shovel")
                .icon("gold_shovel.png").maxStackSize(1).durability(32)
                .toolType(ToolType.SHOVEL).toolTier(ToolTier.GOLD).build());
        register(ItemDefinition.builder().id(1018).name("gold_hoe").displayName("Gold Hoe")
                .icon("gold_hoe.png").maxStackSize(1).durability(32)
                .toolType(ToolType.HOE).toolTier(ToolTier.GOLD).build());
        register(ItemDefinition.builder().id(1019).name("gold_sword").displayName("Gold Sword")
                .icon("gold_sword.png").maxStackSize(1).durability(32)
                .toolType(ToolType.SWORD).toolTier(ToolTier.GOLD).build());
        
        // Diamond tools
        register(ItemDefinition.builder().id(1020).name("diamond_pickaxe").displayName("Diamond Pickaxe")
                .icon("diamond_pickaxe.png").maxStackSize(1).durability(1561)
                .toolType(ToolType.PICKAXE).toolTier(ToolTier.DIAMOND).build());
        register(ItemDefinition.builder().id(1021).name("diamond_axe").displayName("Diamond Axe")
                .icon("diamond_axe.png").maxStackSize(1).durability(1561)
                .toolType(ToolType.AXE).toolTier(ToolTier.DIAMOND).build());
        register(ItemDefinition.builder().id(1022).name("diamond_shovel").displayName("Diamond Shovel")
                .icon("diamond_shovel.png").maxStackSize(1).durability(1561)
                .toolType(ToolType.SHOVEL).toolTier(ToolTier.DIAMOND).build());
        register(ItemDefinition.builder().id(1023).name("diamond_hoe").displayName("Diamond Hoe")
                .icon("diamond_hoe.png").maxStackSize(1).durability(1561)
                .toolType(ToolType.HOE).toolTier(ToolTier.DIAMOND).build());
        register(ItemDefinition.builder().id(1024).name("diamond_sword").displayName("Diamond Sword")
                .icon("diamond_sword.png").maxStackSize(1).durability(1561)
                .toolType(ToolType.SWORD).toolTier(ToolTier.DIAMOND).build());
    }
    
    private void registerMaterials() {
        register(ItemDefinition.builder().id(1025).name("stick").displayName("Stick")
                .icon("stick.png").maxStackSize(64).fuelBurnTime(100).build());
        register(ItemDefinition.builder().id(1026).name("coal").displayName("Coal")
                .icon("coal.png").maxStackSize(64).fuelBurnTime(1600).build());
        register(ItemDefinition.builder().id(1027).name("iron_ingot").displayName("Iron Ingot")
                .icon("iron_ingot.png").maxStackSize(64).build());
        register(ItemDefinition.builder().id(1028).name("gold_ingot").displayName("Gold Ingot")
                .icon("gold_ingot.png").maxStackSize(64).build());
        register(ItemDefinition.builder().id(1029).name("diamond").displayName("Diamond")
                .icon("diamond.png").maxStackSize(64).build());
        register(ItemDefinition.builder().id(1030).name("redstone").displayName("Redstone")
                .icon("redstone.png").maxStackSize(64).build());
        register(ItemDefinition.builder().id(1031).name("emerald").displayName("Emerald")
                .icon("emerald.png").maxStackSize(64).build());
    }
    
    private void registerFood() {
        register(ItemDefinition.builder().id(1050).name("apple").displayName("Apple")
                .icon("apple.png").maxStackSize(64).foodValue(4).build());
        register(ItemDefinition.builder().id(1051).name("bread").displayName("Bread")
                .icon("bread.png").maxStackSize(64).foodValue(5).build());
        register(ItemDefinition.builder().id(1052).name("raw_meat").displayName("Raw Meat")
                .icon("raw_meat.png").maxStackSize(64).foodValue(3).build());
        register(ItemDefinition.builder().id(1053).name("cooked_meat").displayName("Cooked Meat")
                .icon("cooked_meat.png").maxStackSize(64).foodValue(8).build());
    }
    
    private void registerBlockItems() {
        // Basic blocks
        register(ItemDefinition.builder().id(1060).name("stone").displayName("Stone")
                .icon("stone_item.png").maxStackSize(64).placeableBlock((short) 1).build());
        register(ItemDefinition.builder().id(1061).name("cobblestone").displayName("Cobblestone")
                .icon("cobblestone_item.png").maxStackSize(64).placeableBlock((short) 2).build());
        register(ItemDefinition.builder().id(1062).name("stone_bricks").displayName("Stone Bricks")
                .icon("stone_bricks_item.png").maxStackSize(64).placeableBlock((short) 3).build());
        register(ItemDefinition.builder().id(1063).name("dirt").displayName("Dirt")
                .icon("dirt_item.png").maxStackSize(64).placeableBlock((short) 4).build());
        register(ItemDefinition.builder().id(1064).name("grass").displayName("Grass Block")
                .icon("grass_item.png").maxStackSize(64).placeableBlock((short) 5).build());
        register(ItemDefinition.builder().id(1065).name("sand").displayName("Sand")
                .icon("sand_item.png").maxStackSize(64).placeableBlock((short) 6).build());
        register(ItemDefinition.builder().id(1066).name("gravel").displayName("Gravel")
                .icon("gravel_item.png").maxStackSize(64).placeableBlock((short) 7).build());
        
        // Wood blocks
        register(ItemDefinition.builder().id(1067).name("oak_log").displayName("Oak Log")
                .icon("oak_log_item.png").maxStackSize(64).placeableBlock((short) 8).fuelBurnTime(300).build());
        register(ItemDefinition.builder().id(1068).name("oak_planks").displayName("Oak Planks")
                .icon("oak_planks_item.png").maxStackSize(64).placeableBlock((short) 9).fuelBurnTime(300).build());
        register(ItemDefinition.builder().id(1069).name("oak_leaves").displayName("Oak Leaves")
                .icon("oak_leaves_item.png").maxStackSize(64).placeableBlock((short) 10).build());
        
        // Ores
        register(ItemDefinition.builder().id(1070).name("coal_ore").displayName("Coal Ore")
                .icon("coal_ore_item.png").maxStackSize(64).placeableBlock((short) 12).build());
        register(ItemDefinition.builder().id(1071).name("iron_ore").displayName("Iron Ore")
                .icon("iron_ore_item.png").maxStackSize(64).placeableBlock((short) 13).build());
        register(ItemDefinition.builder().id(1072).name("gold_ore").displayName("Gold Ore")
                .icon("gold_ore_item.png").maxStackSize(64).placeableBlock((short) 14).build());
        register(ItemDefinition.builder().id(1073).name("diamond_ore").displayName("Diamond Ore")
                .icon("diamond_ore_item.png").maxStackSize(64).placeableBlock((short) 15).build());
        register(ItemDefinition.builder().id(1074).name("redstone_ore").displayName("Redstone Ore")
                .icon("redstone_ore_item.png").maxStackSize(64).placeableBlock((short) 16).build());
        register(ItemDefinition.builder().id(1075).name("emerald_ore").displayName("Emerald Ore")
                .icon("emerald_ore_item.png").maxStackSize(64).placeableBlock((short) 17).build());
        
        // Special blocks
        register(ItemDefinition.builder().id(1080).name("glass").displayName("Glass")
                .icon("glass_item.png").maxStackSize(64).placeableBlock((short) 18).build());
        register(ItemDefinition.builder().id(1081).name("crafting_table").displayName("Crafting Table")
                .icon("crafting_table_item.png").maxStackSize(64).placeableBlock((short) 21).fuelBurnTime(300).build());
        register(ItemDefinition.builder().id(1082).name("furnace").displayName("Furnace")
                .icon("furnace_item.png").maxStackSize(64).placeableBlock((short) 22).build());
        register(ItemDefinition.builder().id(1083).name("chest").displayName("Chest")
                .icon("chest_item.png").maxStackSize(64).placeableBlock((short) 23).fuelBurnTime(300).build());
        register(ItemDefinition.builder().id(1084).name("torch").displayName("Torch")
                .icon("torch_item.png").maxStackSize(64).placeableBlock((short) 24).build());
    }
    
    public void register(ItemDefinition item) {
        items.put(item.getId(), item);
        itemsByName.put(item.getName(), item);
        if (item.getId() != 0) {  // Don't log AIR
            LOGGER.fine("Registered item: " + item.getName() + " (ID " + item.getId() + ")");
        }
    }
    
    public ItemDefinition getItem(int id) {
        return items.getOrDefault(id, AIR);
    }
    
    public ItemDefinition getItem(String name) {
        return itemsByName.get(name);
    }
    
    public Collection<ItemDefinition> getAllItems() {
        return items.values();
    }
    
    public int getItemCount() {
        return items.size();
    }
}
