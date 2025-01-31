package mods.cybercat.gigeresque.common.entity;

import static java.lang.Math.min;

import mods.cybercat.gigeresque.common.entity.impl.RunnerbursterEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

public interface Growable {
	float getGrowth();

	void setGrowth(float growth);

	float getMaxGrowth();

	default void grow(LivingEntity entity, float amount) {
		setGrowth(min(getGrowth() + amount, getMaxGrowth()));

		if (getGrowth() >= getMaxGrowth()) {
			growUp(entity);
		}
	}

	LivingEntity growInto();

	default void growUp(LivingEntity entity) {
		World world = entity.world;
		if (!world.isClient()) {
			var newEntity = growInto();
			if (newEntity == null)
				return;
			newEntity.refreshPositionAndAngles(entity.getBlockPos(), entity.getYaw(), entity.getPitch());
			if (newEntity instanceof RunnerbursterEntity)
				((RunnerbursterEntity)newEntity).setBirthStatus(false);
			world.spawnEntity(newEntity);
			entity.remove(Entity.RemovalReason.DISCARDED);
		}
	}

	default float getGrowthNeededUntilGrowUp() {
		return getMaxGrowth() - getGrowth();
	}

	default float getGrowthMultiplier() {
		return 1.0f;
	}
}
