package me.aleksilassila.islands.commands.GUIs;

import me.aleksilassila.islands.IslandLayout;
import me.aleksilassila.islands.Main;
import me.aleksilassila.islands.utils.BiomeMaterials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VisitGui implements IVisitGui {
    private final Main plugin;

    private final int INVENTORY_SIZE = 9 * 2;
    private final int WHITESPACE = 0;
    private final int ISLANDS_ON_PAGE = INVENTORY_SIZE - WHITESPACE - 9;

    private final Material TOOLBAR_FILL_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;

    private final int PREVIOUS_PAGE_PADDING = 3;
    private final int NEXT_PAGE_PADDING = 5;

    private int page = 0;
    private int sort = 0;

    public VisitGui(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return getInventoryPage();
    }

    @Override
    public void onInventoryClick(Player player, int slot, ItemStack clickedItem, InventoryView inventoryView) {
        if(clickedItem == null || clickedItem.getType().equals(Material.AIR) || clickedItem.getType().equals(TOOLBAR_FILL_MATERIAL))
            return;

        if (slot < ISLANDS_ON_PAGE) {
            player.closeInventory();
            player.performCommand("visit " +  ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName()));
        } else if (slot - ISLANDS_ON_PAGE - WHITESPACE == PREVIOUS_PAGE_PADDING) {
            player.openInventory(this.setPage(parsePage(inventoryView.getTitle()) - 1).getInventory());
        } else if (slot - ISLANDS_ON_PAGE - WHITESPACE == NEXT_PAGE_PADDING) {
            player.openInventory(this.setPage(parsePage(inventoryView.getTitle()) + 1).getInventory());
        } else if (slot - ISLANDS_ON_PAGE - WHITESPACE == 4) {
            int i = parseSort(inventoryView.getTitle());
            player.openInventory(this.setPage(parsePage(inventoryView.getTitle())).setSort(i == 0 ? 1 : 0).getInventory());
        }
    }

    private Inventory getInventoryPage() {
        Inventory inv = Bukkit.createInventory(this, INVENTORY_SIZE, "Visit Island - By " + parseSort(sort) + " - [" + page + "]");
        Map<String, Map<String, String>> publicIslands = plugin.islands.layout.getPublicIslands();

        List<String> sortedSet = new ArrayList<>(publicIslands.keySet());

        // Sort islands
        if (sort == 1) { // Sort by date, oldest first
            sortedSet.sort(Comparator.comparingInt(a ->
                    IslandLayout.placement.getIslandIndex(new int[]{Integer.parseInt(a.split("x")[0]), Integer.parseInt(a.split("x")[1])})));
        } else { // Sort by name
            sortedSet.sort(Comparator.comparingInt(a -> publicIslands.get(a).get("name").charAt(0)));
        }

        // Add islands to inventory
        int index = 0;
        int startIndex = ISLANDS_ON_PAGE * page;
        for (String islandId : sortedSet) {
            if (index < startIndex || index >= startIndex + ISLANDS_ON_PAGE) {
                index++;
                continue;
            }

            String displayName;

            try {
                displayName = Bukkit.getPlayer(UUID.fromString(publicIslands.get(islandId).get("owner"))).getDisplayName();
            } catch (Exception e) {
                displayName = "Server";
            }

            inv.addItem(createGuiItem(BiomeMaterials.valueOf(publicIslands.get(islandId).get("material")).getMaterial(),
                    ChatColor.GOLD + publicIslands.get(islandId).get("name"),
                    displayName.equals("Server"),
                    ChatColor.GRAY + "By " + displayName));
            index++;
        }

        // Add toolbar
        if ((page + 1) * ISLANDS_ON_PAGE < publicIslands.size()) {
            inv.setItem(ISLANDS_ON_PAGE + WHITESPACE + NEXT_PAGE_PADDING,
                    createGuiItem(Material.ARROW, ChatColor.GOLD + "Next page", false));
        }

        if (page > 0) {
            inv.setItem(ISLANDS_ON_PAGE + WHITESPACE + PREVIOUS_PAGE_PADDING,
                    createGuiItem(Material.ARROW, ChatColor.GOLD + "Previous page", false));
        }

        inv.setItem(ISLANDS_ON_PAGE + WHITESPACE + 4,
                createGuiItem(Material.REDSTONE, ChatColor.GOLD + "Sort by " + parseSort(sort == 1 ? 0 : 1), false));

        // Fill empty toolbar slots
        for (int toolbarIndex = 0; toolbarIndex < 9; toolbarIndex++) {
            if (inv.getItem(INVENTORY_SIZE - 9 + toolbarIndex) == null)
                inv.setItem(INVENTORY_SIZE - 9 + toolbarIndex, createGuiItem(TOOLBAR_FILL_MATERIAL, "", false));
        }

        return inv;
    }

    protected ItemStack createGuiItem(final Material material, final String name, boolean shiny, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        if (shiny) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        meta.setDisplayName(name);

        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);

        return item;
    }

    private int parsePage(String text) {
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        matcher.find();
        return Integer.parseInt(matcher.group());
    }

    private int parseSort(String text) {
        if (text.contains("date")) return 1;
        else return 0;
    }

    private String parseSort(int i) {
        if (i == 1) return "date";
        else return "name";
    }

    public VisitGui setPage(int page) {
        this.page = Math.max(page, 0);

        return this;
    }

    public VisitGui setSort(int sort) {
        this.sort = sort;

        return this;
    }

    public Inventory getDefaultInventory() {
        return setSort(1).setPage(0).getInventory();
    }
}