package me.rockyhawk.commandpanels.openpanelsmanager;

import me.rockyhawk.commandpanels.CommandPanels;
import me.rockyhawk.commandpanels.api.Panel;
import me.rockyhawk.commandpanels.ioclasses.legacy.MinecraftVersions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class OpenGUI {
    CommandPanels plugin;
    public OpenGUI(CommandPanels pl) {
        this.plugin = pl;
    }

    @SuppressWarnings("deprecation")
    public Inventory openGui(Panel panel, Player p, PanelPosition position, PanelOpenType openType, int animateValue) {
        ConfigurationSection pconfig = panel.getConfig();

        Inventory i;
        if(position == PanelPosition.Top) {
            String title = getTitle(p, pconfig, panel, position, animateValue);

            if (isNumeric(pconfig.getString("rows"))) {
                i = Bukkit.createInventory(p, Integer.parseInt(pconfig.getString("rows")) * 9, title);
            } else {
                i = Bukkit.createInventory(p, InventoryType.valueOf(pconfig.getString("rows")), title);
            }
        }else{
            i = p.getInventory();
            //if middle or bottom position, old items need to be cleared
            for (int c = 0; getInvSize(i,position) > c; ++c) {
                if(pconfig.getConfigurationSection("item").getKeys(false).contains(String.valueOf(c))){
                    continue;
                }
                setItem(null, c, i, p, position);
            }
        }

        Set<String> itemList = pconfig.getConfigurationSection("item").getKeys(false);
        HashSet<Integer> takenSlots = new HashSet<>();
        for (String item : itemList) {
            String section;
            //openType needs to not be 3 so the editor won't include hasperm and hasvalue, etc items
            section = plugin.has.hasSection(panel,position,pconfig.getConfigurationSection("item." + item), p);
            //This section is for animations below here: VISUAL ONLY

            //check for if there is animations inside the items section
            if (pconfig.contains("item." + item + section + ".animate" + animateValue)) {
                //check for if it contains the animate that has the animvatevalue
                if (pconfig.contains("item." + item + section + ".animate" + animateValue)) {
                    section = section + ".animate" + animateValue;
                }
            }

            ItemStack s = plugin.itemCreate.makeItemFromConfig(panel,position,Objects.requireNonNull(pconfig.getConfigurationSection("item." + item + section)), p, true, true, true);

            //This is for CUSTOM ITEMS
            if(pconfig.contains("item." + item + section + ".itemType")) {
                //this is for contents in the itemType section
                if (pconfig.getStringList("item." + item + section + ".itemType").contains("placeable") && openType == PanelOpenType.Refresh) {
                    //keep item the same, openType == 0 meaning panel is refreshing
                    setItem(p.getOpenInventory().getItem(Integer.parseInt(plugin.tex.placeholdersNoColour(panel,position,p,item))), item, i, p, position);
                    takenSlots.add(Integer.parseInt(plugin.tex.placeholdersNoColour(panel,position,p,item)));
                    continue;
                }
            }

            try {
                //place item into the GUI
                setItem(s,item,i,p,position);
                takenSlots.add(Integer.parseInt(plugin.tex.placeholdersNoColour(panel,position,p,item)));
                //only place duplicate items in without the editor mode. These are merely visual and will not carry over commands
                if(pconfig.contains("item." + item + section + ".duplicate")) {
                    try {
                        String[] duplicateItems = pconfig.getString("item." + item + section + ".duplicate").split(",");
                        for (String tempDupe : duplicateItems) {
                            if (tempDupe.contains("-")) {
                                //if there is multiple dupe items, convert numbers to ints
                                int[] bothNumbers = new int[]{Integer.parseInt(tempDupe.split("-")[0]), Integer.parseInt(tempDupe.split("-")[1])};
                                for(int n = bothNumbers[0]; n <= bothNumbers[1]; n++){
                                    try{
                                        if(!pconfig.contains("item." + n)){
                                            setItem(s,n,i,p,position);
                                            takenSlots.add(n);
                                        }
                                    }catch(NullPointerException ignore){
                                        setItem(s,n,i,p,position);
                                        takenSlots.add(n);
                                    }
                                }
                            } else {
                                //if there is only one dupe item
                                try{
                                    if(!pconfig.contains("item." + Integer.parseInt(tempDupe))){
                                        setItem(s,Integer.parseInt(tempDupe),i,p,position);
                                        takenSlots.add(Integer.parseInt(tempDupe));
                                    }
                                }catch(NullPointerException ignore){
                                    setItem(s,Integer.parseInt(tempDupe),i,p,position);
                                    takenSlots.add(Integer.parseInt(tempDupe));
                                }
                            }
                        }
                    }catch(NullPointerException nullp){
                        plugin.debug(nullp,p);
                        p.closeInventory();
                        plugin.openPanels.closePanelForLoader(p.getName(),position);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException ignore) {}
        }
        if (pconfig.contains("empty") && !plugin.tex.placeholdersNoColour(panel,position,p,pconfig.getString("empty")).equals("AIR")) {
            String emptyValue = plugin.tex.placeholdersNoColour(panel,position,p,pconfig.getString("empty"));
            ItemStack empty;
            try {
                //emptyID for older versions of minecraft (might be deprecated later on)
                short id = 0;
                if(pconfig.contains("emptyID")){
                    id = Short.parseShort(plugin.tex.placeholdersNoColour(panel,position,p,pconfig.getString("emptyID")));
                }
                //either use custom item or just material type
                if(pconfig.contains("custom-item." + emptyValue)){
                    empty = plugin.itemCreate.makeItemFromConfig(panel,position,pconfig.getConfigurationSection("custom-item." + emptyValue),p,true,true,true);
                }else{
                    empty = new ItemStack(Objects.requireNonNull(Material.matchMaterial(emptyValue.toUpperCase())), 1,id);
                    ItemMeta renamedMeta = empty.getItemMeta();
                    assert renamedMeta != null;
                    renamedMeta.setDisplayName(" ");
                    empty.setItemMeta(renamedMeta);
                }
                if (empty.getType() != Material.AIR) {
                    for (int c = 0; getInvSize(i,position) > c; ++c) {
                        if (!takenSlots.contains(c)) {
                            //place empty item
                            setItem(empty,c,i,p,position);
                        }
                    }
                }
            } catch (IllegalArgumentException | NullPointerException var26) {
                plugin.debug(var26,p);
            }
        }
        if (openType == PanelOpenType.Normal) {
            //declare old panel closed
            if(plugin.openPanels.hasPanelOpen(p.getName(),position)){
                plugin.openPanels.getOpenPanel(p.getName(),position).isOpen = false;
            }
            //open new panel
            plugin.openPanels.skipPanelClose.add(p.getName());
            plugin.openPanels.openPanelForLoader(p.getName(),panel,position);
            //only if it needs to open the top inventory
            if(position == PanelPosition.Top) {
                p.openInventory(i);
            }
            plugin.openPanels.skipPanelClose.remove(p.getName());
        } else if (openType == PanelOpenType.Refresh) {
            //openType Refresh will just refresh the panel
            if(plugin.legacy.MAJOR_VERSION.greaterThanOrEqualTo(MinecraftVersions.v1_21) ||
                    (plugin.legacy.MAJOR_VERSION.greaterThanOrEqualTo(MinecraftVersions.v1_20) && plugin.legacy.MINOR_VERSION >= 5)){
                //Title refresh ability added in 1.20.5 api
                if(position == PanelPosition.Top) {
                    p.getOpenInventory().setTitle(getTitle(p, pconfig, panel, position, animateValue));
                }
            }
            if(position == PanelPosition.Top) {
                plugin.legacy.setStorageContents(p, plugin.legacy.getStorageContents(i));
            }
        } else if (openType == PanelOpenType.Return) {
            //will return the inventory, not opening it at all
            return i;
        }
        return i;
    }

    private int getInvSize(Inventory inv, PanelPosition position){
        if(position == PanelPosition.Top){
            return inv.getSize();
        }else if(position == PanelPosition.Middle){
            return 27;
        }else{
            return 9;
        }
    }

    //allows for a string to be used instead of integer, for placeholders
    private void setItem(ItemStack item, String slotName, Inventory inv, Player p, PanelPosition position) throws ArrayIndexOutOfBoundsException{
        int slot = Integer.parseInt(plugin.tex.placeholdersNoColour(null, position, p, slotName));
        setItem(item, slot, inv, p, position);
    }
    private void setItem(ItemStack item, int slot, Inventory inv, Player p, PanelPosition position) throws ArrayIndexOutOfBoundsException{
        if(position == PanelPosition.Top){
            inv.setItem(slot, item);
        }else if(position == PanelPosition.Middle){
            if(slot+9 < 36) {
                p.getInventory().setItem(slot + 9, item);
            }
        }else{
            if(slot < 9) {
                p.getInventory().setItem(slot, item);
            }
        }
    }

    private boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            int unused = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private String getTitle(Player p, ConfigurationSection pconfig, Panel panel, PanelPosition position, Integer animateValue){
        String title;
        if(pconfig.contains("custom-title")) {
            //used for titles in the custom-title section, for has sections
            String section = plugin.has.hasSection(panel,position,pconfig.getConfigurationSection("custom-title"), p);

            //check for if there is animations inside the custom-title section
            if (pconfig.contains("custom-title" + section + ".animate" + animateValue)) {
                section = section + ".animate" + animateValue;
            }

            title = plugin.tex.placeholders(panel, position, p, pconfig.getString("custom-title" + section + ".title"));
        }else {
            //regular inventory title
            title = plugin.tex.placeholders(panel, position, p, pconfig.getString("title"));
        }
        return title;
    }
}
