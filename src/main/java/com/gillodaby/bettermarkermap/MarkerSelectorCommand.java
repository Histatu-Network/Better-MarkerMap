package com.gillodaby.bettermarkermap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class MarkerSelectorCommand extends AbstractPlayerCommand {

    private static final String PREFIX = "[BetterMarkerMap] ";

    private final MarkerPermissionPacketFilter markerFilter;

    MarkerSelectorCommand(MarkerPermissionPacketFilter markerFilter) {
        super("marker", "Open marker icon selector");
        this.markerFilter = markerFilter;
        // Update 6 removed canGeneratePermission(); this class overrode it to false. Deleting the
        // override is NOT the migration here, because a command that declares nothing gets a node
        // of its own -- /marker would end up gated behind
        // "gillodaby.better_markermap.command.marker", which nobody holds, on top of the check it
        // already performs. requireNoPermission() is the replacement the 0.6 javadoc scopes to
        // exactly this shape: "Say it for a command that hands out the first permission on a
        // server, or one that guards itself by some other means, and say why."
        //
        // Why: /marker guards itself in execute() with markerFilter.canUseMarkerUi(...), i.e.
        // BetterMarkerMapPermissions.PERM_MARKER_UI ("bettermarkermap.marker.ui"). That is the
        // node this plugin documents and that server owners grant. The engine gate would be a
        // second, undocumented one.
        //
        // /markermap deliberately does NOT do this: it never overrode canGeneratePermission and
        // has no internal check, so it keeps its generated node exactly as on 0.5.
        requireNoPermission();
    }

    @Override
    protected void execute(CommandContext context,
                           Store<EntityStore> store,
                           Ref<EntityStore> ref,
                           PlayerRef playerRef,
                           World world) {
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        if (!this.markerFilter.canUseMarkerUi(playerRef.getUuid())) {
            context.sendMessage(Message.raw(PREFIX + "You do not have permission to use /marker ("
                    + BetterMarkerMapPermissions.PERM_MARKER_UI + ")."));
            return;
        }

        this.markerFilter.reloadAvailableMarkerImages();
        player.getPageManager().openCustomPage(ref, store, (CustomUIPage) new MarkerSelectorPage(playerRef, this.markerFilter));
    }
}
