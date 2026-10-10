package dev.blocklens.fabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Used only with ClientLevel's TransientEntitySectionManager<Entity>. */
@Mixin(TransientEntitySectionManager.class)
public interface EntitySectionStorageAccess {
    @Accessor("sectionStorage")
    EntitySectionStorage<Entity> blocklens$sections();
}
