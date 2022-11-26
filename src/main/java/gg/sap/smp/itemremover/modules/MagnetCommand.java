package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MagnetCommand implements CommandExecutor {

    public static final double ACTIVATION_RANGE_SQUARED = 1;
    public static final double MAX_RANGE = 1000;

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        final Format format = new Format(sender);
        if (!(sender instanceof Player player)) {
            format.error("this command is only intended for players.");
            return true;
        }

        if (args.length == 0) {
            format.custom("syntax", "/magnet <radius> [types...]");
            return true;
        }

        final double radius;
        try {
            radius = Math.min(MAX_RANGE, Double.parseDouble(args[0]));
        } catch (final NumberFormatException nfex) {
            format.error("cannot parse radius to double");
            return true;
        }

        final Set<Material> materials = new HashSet<>();
        if (args.length > 1) {
            for (final String materialName : Arrays.stream(Arrays.copyOfRange(args, 1, args.length))
                    .flatMap((Function<String, Stream<String>>) s -> Stream.of(s.split(",")))
                    .map(String::toUpperCase)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet())) {
                final Material material = Material.getMaterial(materialName);
                if (material == null) {
                    format.error("material '&7" + materialName + "&r' not found.");
                    return true;
                }
                materials.add(material);
            }
        }

        long teleportedCount = 0;
        for (final Item item : player.getWorld().getNearbyEntitiesByType(Item.class, player.getLocation(), radius)) {
            format.verbose("&dfound: " + item);
            // check type
            final ItemStack stack = item.getItemStack();
            if (!materials.isEmpty() && !materials.contains(stack.getType())) {
                continue;
            }
            // check distance
            final Location location = item.getLocation();
            if (location.distanceSquared(player.getLocation()) < ACTIVATION_RANGE_SQUARED) {
                continue;
            }
            // cute particles UwU
            location.getWorld().spawnParticle(Particle.END_ROD, location, 2);
            // teleport item to player
            item.teleport(player.getLocation());
            teleportedCount += stack.getAmount();
        }

        format.info("&rteleported &e" + teleportedCount + "&r items");
        return true;
    }

}
