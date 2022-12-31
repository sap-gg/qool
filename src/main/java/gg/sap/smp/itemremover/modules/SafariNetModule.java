package gg.sap.smp.itemremover.modules;

import com.google.common.base.Strings;
import gg.sap.smp.itemremover.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SafariNetModule implements Listener {

    public static final NamespacedKey KEY = new NamespacedKey("sap-gg", "safari-net-valid");
    public static final NamespacedKey ENTITY = new NamespacedKey("sap-gg", "safari-net-status");

    private final ItemStack safariNetItem;
    private final Set<EntityType> blacklistedEntityTypes;

    public SafariNetModule(final JavaPlugin plugin) {
        // create item
        this.safariNetItem = new ItemStack(Material.LEAD);
        {
            final ItemMeta meta = this.safariNetItem.getItemMeta();
            meta.displayName(Component.text("Safari Net", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("Empty", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
            this.safariNetItem.setItemMeta(meta);
        }

        // fill blacklist
        this.blacklistedEntityTypes = new HashSet<>(Arrays.asList(
                EntityType.ENDER_DRAGON, EntityType.ARMOR_STAND,
                EntityType.PLAYER
        ));

        // create crafting recipe
        plugin.getServer().addRecipe(
                new ShapedRecipe(new NamespacedKey(plugin, "safari-net-recipe"), this.safariNetItem)
                        .shape("i|i", "|b|", "ses")
                        .setIngredient('i', Material.IRON_INGOT)
                        .setIngredient('|', Material.STICK)
                        .setIngredient('b', Material.SLIME_BALL)
                        .setIngredient('s', Material.STRING)
                        .setIngredient('e', Material.EMERALD)
        );

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityClick(final PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof LivingEntity entity)
                || entity instanceof Player) {
            return;
        }

        // check if item was empty safari net item
        final Player player = event.getPlayer();
        final ItemStack stack = player.getInventory().getItem(event.getHand());
        if (!stack.hasItemMeta() || !stack.getItemMeta().getPersistentDataContainer().has(KEY)) {
            return;
        }
        // prevent unwanted behaviour
        event.setCancelled(true);

        // check if entity is blacklisted
        if (this.blacklistedEntityTypes.contains(entity.getType())) {
            Format.warn(player, "this entity is blacklisted");
            return;
        }

        // set item to newly created and give/drop
        final ItemStack newSafariItem = this.safariNetItem.clone();
        final ItemMeta meta = newSafariItem.getItemMeta();

        // save entity to item
        final byte[] data = Bukkit.getUnsafe().serializeEntity(entity);
        meta.getPersistentDataContainer().set(ENTITY, PersistentDataType.BYTE_ARRAY, data);

        // add lore with entity type and entity name
        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text(entity.getType().name(), NamedTextColor.YELLOW));
        if (entity.customName() != null) {
            lore.add(entity.customName());
        }

        // add health bar
        {
            final AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attribute != null) {
                lore.add(SafariNetModule.drawHealthBar(entity.getHealth(), attribute.getValue()));
            }
        }

        meta.lore(lore);

        newSafariItem.setItemMeta(meta);

        if (stack.getAmount() == 1) {
            player.getInventory().setItem(event.getHand(), newSafariItem);
        } else {
            // decrease item in hand
            stack.setAmount(stack.getAmount() - 1);
            // add item to inventory
            player.getInventory().setItem(event.getHand(), stack);
            // or drop to world if inventory full
            player.getInventory().addItem(newSafariItem).values().forEach(notAdded ->
                    player.getWorld().dropItem(player.getLocation(), notAdded));
        }

        // un-alive entity
        entity.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, entity.getLocation(), 1);
        entity.remove();
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack stack = player.getInventory().getItem(event.getHand());
        if (!stack.hasItemMeta()) {
            return;
        }
        final PersistentDataContainer data = stack.getItemMeta().getPersistentDataContainer();
        if (!data.has(KEY) || !data.has(ENTITY)) {
            return;
        }

        // deserialize entity from data
        final byte[] entityBytes = data.get(ENTITY, PersistentDataType.BYTE_ARRAY);
        final Entity entity = Bukkit.getUnsafe().deserializeEntity(entityBytes, player.getWorld());
        if (entity == null) {
            Format.error(player, "deserialized entity was null");
            return;
        }

        final Location spawnLocation;
        if (event.getInteractionPoint() != null) {
            spawnLocation = event.getInteractionPoint();
        } else if (event.getClickedBlock() != null) {
            spawnLocation = event.getClickedBlock().getLocation().clone().add(0, .5, 0);
        } else {
            spawnLocation = player.getLocation();
        }

        // spawn entity
        entity.spawnAt(event.getInteractionPoint(), CreatureSpawnEvent.SpawnReason.EGG);
        player.getWorld().spawnParticle(Particle.TOTEM, spawnLocation, 2);

        // remove item
        if (stack.getAmount() == 1) {
            player.getInventory().setItem(event.getHand(), null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            player.getInventory().setItem(event.getHand(), stack);
        }
    }

    private static Component drawHealthBar(final double health, final double maxHealth) {
        final double heartCount = health / 2;

        final int fullHeartCount = (int) Math.floor(heartCount);
        Component component = Component.text(
                Strings.repeat("♥", fullHeartCount),
                NamedTextColor.DARK_RED
        );
        int totalAdded = fullHeartCount;

        // add half heart
        if (heartCount - fullHeartCount >= .5) {
            component = component.append(Component.text(
                    "❥",
                    NamedTextColor.RED
            ));
            ++totalAdded;
        }

        return component.append(Component.text(
                Strings.repeat("♥", (int) (Math.floor(maxHealth / 2) - totalAdded)),
                NamedTextColor.GRAY
        ));
    }

}
