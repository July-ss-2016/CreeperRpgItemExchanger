package vip.creeper.mcserverplugins.creeperrpgitemexchanger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import vip.creeper.mcserverplugins.creeperrpgitem.CreeperRpgItem;
import vip.creeper.mcserverplugins.creeperrpgitem.RpgItem;
import vip.creeper.mcserverplugins.creeperrpgitemattributedisplayer.CreeperRpgItemAttributeDisplayer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.List;

/**
 * Created by July_ on 2017/8/5.
 */
public class CreeperRpgItemExchanger extends JavaPlugin implements Listener {
    private CreeperRpgItemExchanger instance;
    public static final String HEAD_MSG = "§a[CreeperRpgItemExchanger] §b";
    public void onEnable() {
        instance = this;
        File logFolder = new File(getDataFolder().getAbsolutePath() + File.separator + "logs");

        if (!logFolder.exists()) {
            getLogger().info("日志文件夹已创建!");
            logFolder.mkdirs();
        }

        getCommand("crie").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("初始化完毕!");
    }

    public boolean onCommand(CommandSender cs, Command cmd, String lable, String[] args) {
        if (cs instanceof Player && cs.hasPermission("CreeperRpgItemExchanger.admin") && args.length == 3 && args[0].equalsIgnoreCase("get") && args[1].equalsIgnoreCase("ei")) {
            Player player = (Player) cs;
            PlayerInventory playerInventory = player.getInventory();
            ItemStack item = getExchangeItem(args[2]);

            if (item == null) {
                player.sendMessage("物品代码不存在!");
                return true;
            } else {
                if (playerInventory.firstEmpty() == -1) {
                    player.sendMessage("背包空间不足!");
                    return true;
                }

                playerInventory.addItem(item);
                player.sendMessage("已添加到背包!");
                return true;
            }
        }
        return false;
    }

    private ItemStack getExchangeItem(String itemCode) {
        if (!CreeperRpgItem.getInstance().getRpgItemManager().isExistsRpgItem(itemCode)) {
            return null;
        }

        RpgItem rpgItem = CreeperRpgItem.getInstance().getRpgItemManager().getRpgItem(itemCode);
        ItemStack item = new ItemStack(rpgItem.getItemStack());
        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.setDisplayName("§c购买后需单击左键兑换为实物才能使用");
        itemMeta.setLore(CreeperRpgItemAttributeDisplayer.getInstance().getItemAttributeInformations(itemCode));

        List<String> tempLores = itemMeta.getLore();

        if (tempLores != null) {
            tempLores.add(0, "§7- §f代码 §b> §fEXCHANGE_" + itemCode);
        }

        itemMeta.setLore(tempLores);
        item.setItemMeta(itemMeta);
        return item;
    }

    @EventHandler
    public void onPlayerInteractEvent(PlayerInteractEvent event) throws IOException {
        Player player = event.getPlayer();
        String playerName = player.getName();
        PlayerInventory playerInventory = player.getInventory();
        ItemStack handItem = player.getItemInHand();

        if (handItem == null || handItem.getType() == Material.AIR) {
            return;
        }

        List<String> itemLores = handItem.getItemMeta().getLore();

        if (itemLores == null || itemLores.size() == 0) {
            return;
        }

        String firstLore = itemLores.get(0);

        if (!firstLore.startsWith("§7- §f代码 §b> §fEXCHANGE_")) {
            return;
        }

        String itemCode = firstLore.replace("§7- §f代码 §b> §fEXCHANGE_", "");

        if (CreeperRpgItem.getInstance().getRpgItemManager().isExistsRpgItem(itemCode)) {
            if (playerInventory.firstEmpty() == -1) {
                player.sendMessage(HEAD_MSG + "§c背包空间不足!");
                return;
            }

            ItemStack tempItem = new ItemStack(CreeperRpgItem.getInstance().getRpgItemManager().getRpgItem(itemCode).getItemStack());

            tempItem.setAmount(handItem.getAmount());
            player.setItemInHand(new ItemStack(Material.AIR)); // 必须清除物品
            playerInventory.addItem(tempItem);
            player.sendMessage(HEAD_MSG + "已兑换成实物,尽情使用吧!");
            getLogger().info("玩家 = " + playerName + " 兑换了 物品 = " + itemCode);

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> Bukkit.getScheduler().runTask(instance, () -> {
                try {
                    addDataToFile(getDataFolder().getAbsolutePath() + File.separator + "logs" + File.separator + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log",
                            "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]player=" + playerName + ",item=" + tempItem.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

        } else {
            player.sendMessage("§c物品代码不存在,请联系管理员处理!");
        }
    }

    public static boolean addDataToFile(String path, String data) throws IOException {
        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, true)));
        bf.write(data+"\n");
        bf.close();
        return false;
    }
}
