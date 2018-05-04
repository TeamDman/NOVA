package com.teamdman.animus.rituals;

import WayofTime.bloodmagic.core.data.SoulNetwork;
import WayofTime.bloodmagic.ritual.*;
import WayofTime.bloodmagic.util.helper.NetworkHelper;
import com.teamdman.animus.Constants;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
/**
 * Created by TeamDman on 2015-05-28.
 */
public class RitualUnmaking extends Ritual {
	public static final String EFFECT_RANGE = "effect";

	public RitualUnmaking() {
		super("ritualUnmaking", 0, 3000, "ritual." + Constants.Mod.MODID + ".unmaking");

		addBlockRange(EFFECT_RANGE, new AreaDescriptor.Rectangle(new BlockPos(-2, -2, -2), 5));
		setMaximumVolumeAndDistanceOfRange(EFFECT_RANGE, 0, 8, 8);
	}

	@Override
	public void performRitual(IMasterRitualStone masterRitualStone) {
		World       world          = masterRitualStone.getWorldObj();
		SoulNetwork network        = NetworkHelper.getSoulNetwork(masterRitualStone.getOwner());
		int         currentEssence = network.getCurrentEssence();
		BlockPos    masterPos      = masterRitualStone.getBlockPos();

		if (!masterRitualStone.getWorldObj().isRemote) {
			if (currentEssence < getRefreshCost()) {
				network.causeNausea();
				return;
			}

			AreaDescriptor   effectRange = getBlockRange(EFFECT_RANGE);
			List<EntityItem> itemList    = world.getEntitiesWithinAABB(EntityItem.class, effectRange.getAABB(masterRitualStone.getBlockPos()));
			if (!itemList.isEmpty()) {
				// find blank books to put enchantments on
				Optional<EntityItem> booksOpt = itemList.stream()
						.filter(e -> !e.isDead)
						.filter(e -> e.getItem().getItem() == Items.BOOK)
						.findFirst();
				if (!booksOpt.isPresent())
					return;
				EntityItem books = booksOpt.get();

				for (EntityItem entityItem : itemList) {
					if (entityItem.getItem().getItem() == Items.ENCHANTED_BOOK) {
						if (!entityItem.getItem().hasTagCompound())
							continue;
						//noinspection ConstantConditions
						NBTTagList enchants = entityItem.getItem().getTagCompound().getTagList("StoredEnchantments", 10);
						if (enchants.hasNoTags())
							continue;
						for (int i = enchants.tagCount() - 1; i >= 0; i--) {
							if (books.getItem().isEmpty())
								break;

							NBTTagCompound data    = enchants.getCompoundTagAt(i);
							short          enchID  = data.getShort("id");
							short          enchLVL = data.getShort("lvl");
							enchants.removeTag(i);

							ItemStack      enchBook        = new ItemStack(Items.ENCHANTED_BOOK);
							NBTTagList     tagEnchantments = new NBTTagList();
							NBTTagCompound tagEnchInfo     = new NBTTagCompound();
							tagEnchInfo.setShort("id", enchID);
							tagEnchInfo.setShort("lvl", (short) (enchLVL > 2 ? enchLVL - 1 : 1));
							tagEnchantments.appendTag(tagEnchInfo);
							NBTTagCompound tag = new NBTTagCompound();
							tag.setTag("StoredEnchantments", tagEnchantments);
							enchBook.setTagCompound(tag);
							world.spawnEntity(new EntityItem(world, masterPos.getX(), masterPos.getY() + 1, masterPos.getZ(), enchBook.copy()));
							world.spawnEntity(new EntityItem(world, masterPos.getX(), masterPos.getY() + 1, masterPos.getZ(), enchBook));

							books.getItem().shrink(1);
						}
						entityItem.getItem().shrink(1);
						world.playSound(null, masterPos, SoundEvents.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, 0.5F, 1.0F);
						masterRitualStone.stopRitual(BreakType.DEACTIVATE);
					} else {
						NBTTagList enchants = entityItem.getItem().getEnchantmentTagList();
						for (int i = enchants.tagCount() - 1; i >= 0; --i) {
							if (!books.getItem().isEmpty()) {
								break;
							}
							ItemStack      enchBook = new ItemStack(Items.ENCHANTED_BOOK);
							NBTTagCompound data     = enchants.getCompoundTagAt(i);
							short          enchID   = data.getShort("id");
							short          enchLVL  = data.getShort("lvl");
							enchants.removeTag(i);
							enchBook.setTagCompound(new NBTTagCompound());
							NBTTagList     bookTags       = new NBTTagList();
							NBTTagCompound nbttagcompound = new NBTTagCompound();
							nbttagcompound.setShort("id", enchID);
							nbttagcompound.setShort("lvl", enchLVL);
							bookTags.appendTag(nbttagcompound);
							NBTTagCompound tag = new NBTTagCompound();
							tag.setTag("StoredEnchantments", bookTags);
							enchBook.setTagCompound(tag);
							world.spawnEntity(new EntityItem(world, masterPos.getX(), masterPos.getY() + 1, masterPos.getZ(), enchBook));
							books.getItem().shrink(1);
						}
						if (entityItem.getItem().getEnchantmentTagList().tagCount() == 0) {
							entityItem.getItem().getTagCompound().removeTag("ench");
						}

						world.playSound(null, masterPos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.5F, 1.0F);
						masterRitualStone.stopRitual(BreakType.DEACTIVATE);
					}
				}
				network.syphon(getRefreshCost());
			}
		}
	}

	@Override
	public int getRefreshCost() {
		return 0;
	}

	@Override
	public int getRefreshTime() {
		return 20;
	}

	@Override
	public void gatherComponents(Consumer<RitualComponent> components) {
		components.accept(new RitualComponent(new BlockPos(-4, 0, -2), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-4, 0, 0), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-4, 0, 2), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-3, 0, -3), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(-3, 0, -1), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-3, 0, 1), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-3, 0, 3), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(-2, 0, -4), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(-2, 0, -2), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(-2, 0, 0), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-2, 0, 2), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(-2, 0, 4), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(-1, 0, -3), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(-1, 0, -1), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(-1, 0, 0), EnumRuneType.FIRE));
		components.accept(new RitualComponent(new BlockPos(-1, 0, 1), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(-1, 0, 3), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(0, 0, -4), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(0, 0, -2), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(0, 0, -1), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(0, 0, 1), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(0, 0, 2), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(0, 0, 4), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(1, 0, -3), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(1, 0, -1), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(1, 0, 0), EnumRuneType.WATER));
		components.accept(new RitualComponent(new BlockPos(1, 0, 1), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(1, 0, 3), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(2, 0, -4), EnumRuneType.AIR));
		components.accept(new RitualComponent(new BlockPos(2, 0, -2), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(2, 0, 0), EnumRuneType.WATER));
		components.accept(new RitualComponent(new BlockPos(2, 0, 2), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(2, 0, 4), EnumRuneType.EARTH));
		components.accept(new RitualComponent(new BlockPos(3, 0, -3), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(3, 0, -1), EnumRuneType.WATER));
		components.accept(new RitualComponent(new BlockPos(3, 0, 1), EnumRuneType.WATER));
		components.accept(new RitualComponent(new BlockPos(3, 0, 3), EnumRuneType.DUSK));
		components.accept(new RitualComponent(new BlockPos(4, 0, -2), EnumRuneType.WATER));
		components.accept(new RitualComponent(new BlockPos(4, 0, 0), EnumRuneType.WATER));
		components.accept(new RitualComponent(new BlockPos(4, 0, 2), EnumRuneType.WATER));
	}

	@Override
	public Ritual getNewCopy() {
		return new RitualUnmaking();
	}

}