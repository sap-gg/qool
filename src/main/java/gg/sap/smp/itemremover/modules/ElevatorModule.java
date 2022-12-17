package gg.sap.smp.itemremover.modules;

import gg.sap.smp.itemremover.util.Format;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.DaylightDetector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class ElevatorModule implements Listener {

    @EventHandler
    public void onMoveElevUp(final PlayerMoveEvent event) {
        // Check if player is going upwards
        if (!(event.getFrom().getY() < event.getTo().getY())) {
            return;
        }
        // Check if player is flying
        if (event.getPlayer().isFlying()) {
            return;
        }
        final Player player = event.getPlayer();
        // Check if player is moving 1/4 Block upwards
        if (event.getTo().getY() - event.getFrom().getY() < 0.25) {
            return;
        }
        final Block block = event.getFrom().getBlock();
        if (block.getBlockData() instanceof DaylightDetector) {
            elevatorUp(player, block);
        }
    }

    @EventHandler
    public void onToggleSneak(final PlayerToggleSneakEvent event) {
        // Check if player is sneaking
        if (!event.isSneaking()) {
            return;
        }
        final Block block = event.getPlayer().getLocation().getBlock();
        if (block.getBlockData() instanceof DaylightDetector) {
            elevatorDown(event.getPlayer(), block);
        }
    }

    private void elevatorUp(final Player player, final Block block) {
        final Location check = block.getLocation().clone();
        for (int y = block.getY(); y < check.getWorld().getMaxHeight(); y++) {
            check.add(0, 1, 0);
            final Block checkBlock = check.getBlock();

            if (!(checkBlock.getBlockData() instanceof DaylightDetector)) {
                continue;
            }

            final IllegalBlock illegalBlock = findIllegalBlock(checkBlock);
            if (illegalBlock != IllegalBlock.NONE && player.getGameMode() == GameMode.SURVIVAL) {
                Format.warn(player, "illegal block &r" + illegalBlock + "&r found. won't teleport.");
                continue;
            }

            player.teleport(player.getLocation().add(0, y - player.getLocation().getY() + 1.4, 0));
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 1.0F, 1.0F);
            break;
        }
    }

    private void elevatorDown(final Player player, final Block block) {
        final Location check = block.getLocation().clone();
        for (int y = block.getY(); y > 0; y--) {
            check.subtract(0, 1, 0);

            final Block checkBlock = check.getBlock();
            if (!(checkBlock.getBlockData() instanceof DaylightDetector)) {
                continue;
            }

            final IllegalBlock illegalBlock = findIllegalBlock(checkBlock);
            if (illegalBlock != IllegalBlock.NONE && player.getGameMode() == GameMode.SURVIVAL) {
                Format.warn(player, "illegal block &r" + illegalBlock + "&r found. won't teleport.");
                continue;
            }

            player.teleport(player.getLocation().subtract(0, player.getLocation().getY() - y + 0.6, 0));
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_TRAPDOOR_OPEN, 1.0F, 1.0F);
            break;
        }
    }

    private IllegalBlock findIllegalBlock(final Block block) {
        final Location check = block.getLocation();
        for (int j = 0; j < 3; j++) {
            final Material material = check.add(0, 1, 0).getBlock().getType();
            if (material == Material.LAVA) {
                return IllegalBlock.LAVA;
            }
            if (material == Material.WATER) {
                return IllegalBlock.WATER;
            }
        }
        return IllegalBlock.NONE;
    }

    public enum IllegalBlock {
        WATER("&9Water"),
        LAVA("&cLava"),
        NONE("&7Nothing");

        final String desc;

        IllegalBlock(final String desc) {
            this.desc = desc;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

}
