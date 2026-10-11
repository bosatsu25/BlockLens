package dev.blocklens.fabric;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Immutable identities only: never retain an atlas sprite across resource reloads. */
public final class BBEBrightChestMaterials {
    private static final SpriteId SINGLE = material("single");
    private static final SpriteId LEFT = material("left");
    private static final SpriteId RIGHT = material("right");

    private BBEBrightChestMaterials() { }

    private static SpriteId material(String part) {
        // BBE resolves SpriteId.texture() in its block atlas rather than the entity atlas.
        return new SpriteId(Sheets.CHEST_SHEET,
                Identifier.fromNamespaceAndPath("blocklens", "entity/chest/bright_" + part));
    }

    public static SpriteId forType(ChestType type) {
        return switch (type) {
            case SINGLE -> SINGLE;
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
        };
    }

    public static boolean owns(Identifier texture) {
        return SINGLE.texture().equals(texture) || LEFT.texture().equals(texture) || RIGHT.texture().equals(texture);
    }
}
