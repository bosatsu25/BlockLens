package dev.blocklens.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Narrow access to already loaded client entity sections; never queries server state. */
@Mixin(ClientLevel.class)
public interface ClientEntityStorageAccess {
    @Accessor("entityStorage")
    TransientEntitySectionManager<Entity> blocklens$entityStorage();
}
