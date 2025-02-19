package mods.cybercat.gigeresque.common.tags;

import mods.cybercat.gigeresque.common.Gigeresque;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class GigTags {
	private GigTags() {
	}

	public static final TagKey<Block> ALIEN_REPELLENTS = TagKey.of(Registry.BLOCK_KEY,
			new Identifier(Gigeresque.MOD_ID, "alien_repellents"));
	public static final TagKey<Block> DESTRUCTIBLE_LIGHT = TagKey.of(Registry.BLOCK_KEY,
			new Identifier(Gigeresque.MOD_ID, "destructible_light"));
	public static final TagKey<Block> ACID_RESISTANT = TagKey.of(Registry.BLOCK_KEY,
			new Identifier(Gigeresque.MOD_ID, "acid_resistant"));
	public static final TagKey<Block> DUNGEON_BLOCKS = TagKey.of(Registry.BLOCK_KEY,
			new Identifier(Gigeresque.MOD_ID, "dungeon_blocks"));
	public static final TagKey<Block> DUNGEON_STAIRS = TagKey.of(Registry.BLOCK_KEY,
			new Identifier(Gigeresque.MOD_ID, "dungeon_stairs"));

	public static final TagKey<Item> BUSTER_FOOD = TagKey.of(Registry.ITEM_KEY,
			new Identifier(Gigeresque.MOD_ID, "buster_food"));
}
