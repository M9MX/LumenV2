package org.m9mx.lumenV2;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.m9mx.lumenV2.command.LumenCommand;
import org.m9mx.lumenV2.command.TrustCommand;
import org.m9mx.lumenV2.command.ItemsCommand;
import org.m9mx.lumenV2.command.ItemsCommandListener;
import org.m9mx.lumenV2.config.ConfigManager;
import org.m9mx.lumenV2.item.ItemRegistry;
import org.m9mx.lumenV2.util.ItemDataHelper; // Corrected import path to util
import org.m9mx.lumenV2.items.etherealkatana.EtherealKatanaAbility;
import org.m9mx.lumenV2.items.lotusblossom.LotusBlossomAbility;
import org.m9mx.lumenV2.items.reinforcedmesh.ReinforcedMeshAbility;
import org.m9mx.lumenV2.items.solstice.SolsticeAbility;
import org.m9mx.lumenV2.items.soulrender.SoulrenderAbility;
import org.m9mx.lumenV2.items.wardenheart.WardenHeartDrop;
import org.m9mx.lumenV2.items.lostsoul.LostSoulObtain;
import org.m9mx.lumenV2.items.awakenedlichblade.AwakenedLichbladeAbility; // Import the new ability
import org.m9mx.lumenV2.items.eternal.EternalSteakAbility;
import org.m9mx.lumenV2.items.eternal.EternalGoldenCarrotAbility;
import org.m9mx.lumenV2.items.eternal.EternalPorkchopAbility;
import org.m9mx.lumenV2.listeners.FirstJoinListener;
import org.m9mx.lumenV2.listeners.RavensMessageDeliveryListener;
import org.m9mx.lumenV2.systems.CooldownBossBarManager;
import org.m9mx.lumenV2.systems.EnhancementSystem;
import org.m9mx.lumenV2.systems.ForgeSystem;
import org.m9mx.lumenV2.systems.ItemProtection;
import org.m9mx.lumenV2.systems.trust.TrustSystem;
import org.m9mx.lumenV2.data.RavensMessageDataManager;
import org.m9mx.lumenV2.util.BundleUtil;
import org.m9mx.lumenV2.util.ItemAutoRegistrar;
import org.m9mx.lumenV2.util.RavensMessageUtil;
import org.m9mx.lumenV2.util.ResourcePackHandler;

public final class Lumen extends JavaPlugin {
    
    private ResourcePackHandler resourcePackHandler;
    private EtherealKatanaAbility etherealKatanaAbility;
    private SolsticeAbility solsticeAbility;
    private SoulrenderAbility soulrenderAbility;
    private LotusBlossomAbility lotusBlossomAbility;
    private WardenHeartDrop wardenHeartDrop;
    private LostSoulObtain lostSoulObtain;
    private BundleUtil bundleUtil;
    private RavensMessageUtil ravensMessageUtil;
    private ReinforcedMeshAbility reinforcedMeshAbility;
    private AwakenedLichbladeAbility awakenedLichbladeAbility; // New ability
    private EternalSteakAbility eternalSteakAbility;
    private EternalGoldenCarrotAbility eternalGoldenCarrotAbility;
    private EternalPorkchopAbility eternalPorkchopAbility;

    @Override
    public void onEnable() {
        // Initialize config manager
        ConfigManager.initialize(this);
        
        // Initialize data managers
        RavensMessageDataManager.initialize(getDataFolder());
        
        // Initialize systems
        ItemProtection.initialize(this);
        EnhancementSystem.initialize(this);
        ForgeSystem.initialize(this);
        TrustSystem.initialize(this);
        
        // Initialize universal systems
        CooldownBossBarManager.initialize(this);
        
        // Initialize item abilities
        this.etherealKatanaAbility = new EtherealKatanaAbility(this);
        this.solsticeAbility = new SolsticeAbility(this);
        this.soulrenderAbility = new SoulrenderAbility(this);
        this.lotusBlossomAbility = new LotusBlossomAbility(this);
        this.reinforcedMeshAbility = new ReinforcedMeshAbility(this);
        this.awakenedLichbladeAbility = new AwakenedLichbladeAbility(this); // Initialize new ability
        this.eternalSteakAbility = new EternalSteakAbility(this);
        this.eternalGoldenCarrotAbility = new EternalGoldenCarrotAbility(this);
        this.eternalPorkchopAbility = new EternalPorkchopAbility(this);
        
        // Register the Soulrender ability listener
        getServer().getPluginManager().registerEvents(this.soulrenderAbility, this);
        getServer().getPluginManager().registerEvents(this.lotusBlossomAbility, this);
        getServer().getPluginManager().registerEvents(this.reinforcedMeshAbility, this);
        getServer().getPluginManager().registerEvents(this.awakenedLichbladeAbility, this); // Register new ability
        getServer().getPluginManager().registerEvents(this.eternalSteakAbility, this);
        getServer().getPluginManager().registerEvents(this.eternalGoldenCarrotAbility, this);
        getServer().getPluginManager().registerEvents(this.eternalPorkchopAbility, this);
        
        // Initialize warden heart drop handler
        this.wardenHeartDrop = new WardenHeartDrop();
        getServer().getPluginManager().registerEvents(wardenHeartDrop, this);
        
        // Initialize Lost Soul obtain handler
        this.lostSoulObtain = new LostSoulObtain();
        getServer().getPluginManager().registerEvents(lostSoulObtain, this);
        
        // Initialize bundle utility and register its events
        this.bundleUtil = new BundleUtil(this);
        getServer().getPluginManager().registerEvents(bundleUtil, this);
        
        // Initialize ravens message utility and register its events
        this.ravensMessageUtil = new RavensMessageUtil(this);
        getServer().getPluginManager().registerEvents(ravensMessageUtil, this);
        
        // Register First Join listener (gives Lumen Guide on first join)
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);
        
        // Register Ravens Message delivery listener
        getServer().getPluginManager().registerEvents(new RavensMessageDeliveryListener(), this);
        
        // Register Items command listener
        getServer().getPluginManager().registerEvents(new ItemsCommandListener(), this);
        
        // Auto-discover and register all CustomItem subclasses
        ItemAutoRegistrar.autoRegisterItems();
        
        // Disable vanilla bundle recipe and register Enhanced Bundle recipe
        disableVanillaBundleRecipe();
        registerEnhancedBundleRecipe();
        
        // Register commands
        registerCommands();
        
        // Log total registered items
        int itemCount = ItemDataHelper.getItemCount();
        getLogger().info("Lumen V2 enabled! Registered " + itemCount + " items.");
        
        // Initialize resource pack handler
        this.resourcePackHandler = new ResourcePackHandler(this);
        getServer().getPluginManager().registerEvents(resourcePackHandler, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Lumen V2 disabled!");
    }
    
    /**
     * Disables the vanilla bundle recipe to force players to use our custom bundle recipe
     */
    private void disableVanillaBundleRecipe() {
        NamespacedKey vanillaBundleKey = NamespacedKey.minecraft("bundle");
        
        // Iterate through all recipes and remove the vanilla bundle recipe
        for (Recipe recipe : Bukkit.getServer().getRecipesFor(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BUNDLE))) {
            if (recipe instanceof Keyed keyedRecipe) {
                if (keyedRecipe.getKey().equals(vanillaBundleKey)) {
                    Bukkit.removeRecipe(keyedRecipe.getKey());
                    getLogger().info("Disabled vanilla bundle recipe");
                    break;
                }
            }
        }
    }
    
    /**
     * Registers the Enhanced Bundle recipe (String above Leather, vertical pattern in any column)
     * Can be crafted anywhere in grid: String directly above Leather (no spaces)
     * Directly produces the custom Enhanced Bundle item with all properties
     */
    private void registerEnhancedBundleRecipe() {
        // Get the custom Enhanced Bundle item and build it
        org.m9mx.lumenV2.item.CustomItem bundleItem = ItemRegistry.getItemStatic("enhanced_bundle");
        if (bundleItem == null) {
            getLogger().warning("Failed to register Enhanced Bundle recipes: item not found in registry!");
            return;
        }
        
        ItemStack result = bundleItem.build();
        
        // Top rows: Column 1 (String at 0, Leather at 3)
        NamespacedKey key1 = new NamespacedKey(this, "enhanced_bundle_top_col1");
        org.bukkit.inventory.ShapedRecipe recipe1 = new org.bukkit.inventory.ShapedRecipe(key1, result);
        recipe1.shape("S  ", "L  ", "   ");
        recipe1.setIngredient('S', org.bukkit.Material.STRING);
        recipe1.setIngredient('L', org.bukkit.Material.LEATHER);
        Bukkit.addRecipe(recipe1);
        
        // Top rows: Column 2 (String at 1, Leather at 4)
        NamespacedKey key2 = new NamespacedKey(this, "enhanced_bundle_top_col2");
        org.bukkit.inventory.ShapedRecipe recipe2 = new org.bukkit.inventory.ShapedRecipe(key2, result);
        recipe2.shape(" S ", " L ", "   ");
        recipe2.setIngredient('S', org.bukkit.Material.STRING);
        recipe2.setIngredient('L', org.bukkit.Material.LEATHER);
        Bukkit.addRecipe(recipe2);
        
        // Top rows: Column 3 (String at 2, Leather at 5)
        NamespacedKey key3 = new NamespacedKey(this, "enhanced_bundle_top_col3");
        org.bukkit.inventory.ShapedRecipe recipe3 = new org.bukkit.inventory.ShapedRecipe(key3, result);
        recipe3.shape("  S", "  L", "   ");
        recipe3.setIngredient('S', org.bukkit.Material.STRING);
        recipe3.setIngredient('L', org.bukkit.Material.LEATHER);
        Bukkit.addRecipe(recipe3);
        
        // Middle rows: Column 1 (String at 3, Leather at 6)
        NamespacedKey key4 = new NamespacedKey(this, "enhanced_bundle_mid_col1");
        org.bukkit.inventory.ShapedRecipe recipe4 = new org.bukkit.inventory.ShapedRecipe(key4, result);
        recipe4.shape("   ", "S  ", "L  ");
        recipe4.setIngredient('S', org.bukkit.Material.STRING);
        recipe4.setIngredient('L', org.bukkit.Material.LEATHER);
        Bukkit.addRecipe(recipe4);
        
        // Middle rows: Column 2 (String at 4, Leather at 7)
        NamespacedKey key5 = new NamespacedKey(this, "enhanced_bundle_mid_col2");
        org.bukkit.inventory.ShapedRecipe recipe5 = new org.bukkit.inventory.ShapedRecipe(key5, result);
        recipe5.shape("   ", " S ", " L ");
        recipe5.setIngredient('S', org.bukkit.Material.STRING);
        recipe5.setIngredient('L', org.bukkit.Material.LEATHER);
        Bukkit.addRecipe(recipe5);
        
        // Middle rows: Column 3 (String at 5, Leather at 8)
        NamespacedKey key6 = new NamespacedKey(this, "enhanced_bundle_mid_col3");
        org.bukkit.inventory.ShapedRecipe recipe6 = new org.bukkit.inventory.ShapedRecipe(key6, result);
        recipe6.shape("   ", "  S", "  L");
        recipe6.setIngredient('S', org.bukkit.Material.STRING);
        recipe6.setIngredient('L', org.bukkit.Material.LEATHER);
        Bukkit.addRecipe(recipe6);
        
        getLogger().info("Registered Enhanced Bundle recipes (6 vertical variants - String above Leather)");
    }
    
    private void registerCommands() {
        LumenCommand lumenCommand = new LumenCommand();
        TrustCommand trustCommand = new TrustCommand();
        ItemsCommand itemsCommand = new ItemsCommand();
        
        // Register the /lumen command programmatically for Paper plugins
        getCommandMap().register("lumen", new org.bukkit.command.Command("lumen") {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                return lumenCommand.onCommand(sender, this, commandLabel, args);
            }
            
            @Override
            public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                java.util.List<String> completions = lumenCommand.onTabComplete(sender, this, alias, args);
                return completions != null ? completions : new java.util.ArrayList<>();
            }
        });
        
        // Register the /trust command programmatically for Paper plugins
        getCommandMap().register("trust", new org.bukkit.command.Command("trust") {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                return trustCommand.onCommand(sender, this, commandLabel, args);
            }
            
            @Override
            public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                java.util.List<String> completions = trustCommand.onTabComplete(sender, this, alias, args);
                return completions != null ? completions : new java.util.ArrayList<>();
            }
        });
        
        // Register the /items command programmatically for Paper plugins
        getCommandMap().register("items", new org.bukkit.command.Command("items") {
            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
                return itemsCommand.onCommand(sender, this, commandLabel, args);
            }
            
            @Override
            public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String alias, String[] args) {
                java.util.List<String> completions = itemsCommand.onTabComplete(sender, this, alias, args);
                return completions != null ? completions : new java.util.ArrayList<>();
            }
        });
    }
    
    private org.bukkit.command.CommandMap getCommandMap() {
        return org.bukkit.Bukkit.getCommandMap();
    }
    
    public EtherealKatanaAbility getEtherealKatanaAbility() {
        return etherealKatanaAbility;
    }
    
    public SolsticeAbility getSolsticeAbility() {
        return solsticeAbility;
    }
    
    public SoulrenderAbility getSoulrenderAbility() {
        return soulrenderAbility;
    }
    
    public LotusBlossomAbility getLotusBlossomAbility() {
        return lotusBlossomAbility;
    }
    
    public WardenHeartDrop getWardenHeartDrop() {
        return wardenHeartDrop;
    }
    
    public LostSoulObtain getLostSoulObtain() {
        return lostSoulObtain;
    }
    
    public BundleUtil getBundleUtil() {
        return bundleUtil;
    }
    
    public ReinforcedMeshAbility getReinforcedMeshAbility() {
        return reinforcedMeshAbility;
    }
    
    public AwakenedLichbladeAbility getAwakenedLichbladeAbility() {
        return awakenedLichbladeAbility;
    }
}