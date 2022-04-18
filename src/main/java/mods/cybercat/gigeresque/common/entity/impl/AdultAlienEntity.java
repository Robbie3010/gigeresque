package mods.cybercat.gigeresque.common.entity.impl;

import static java.lang.Math.max;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.Dynamic;

import mods.cybercat.gigeresque.Constants;
import mods.cybercat.gigeresque.common.Gigeresque;
import mods.cybercat.gigeresque.common.entity.AlienEntity;
import mods.cybercat.gigeresque.common.entity.Growable;
import mods.cybercat.gigeresque.common.entity.ai.brain.AdultAlienBrain;
import mods.cybercat.gigeresque.common.entity.ai.brain.memory.MemoryModuleTypes;
import mods.cybercat.gigeresque.common.entity.ai.brain.sensor.SensorTypes;
import mods.cybercat.gigeresque.common.entity.ai.goal.FleeFireGoal;
import mods.cybercat.gigeresque.common.entity.ai.pathing.AmphibiousNavigation;
import mods.cybercat.gigeresque.common.sound.GigSounds;
import mods.cybercat.gigeresque.common.util.EntityUtils;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.control.AquaticMoveControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.control.YawAdjustingLookControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.SpiderNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.tag.TagKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;

public abstract class AdultAlienEntity extends AlienEntity implements IAnimatable, Growable, IAnimationTickable {

	private static final TrackedData<Float> GROWTH = DataTracker.registerData(AdultAlienEntity.class,
			TrackedDataHandlerRegistry.FLOAT);
	private static final TrackedData<Boolean> IS_HISSING = DataTracker.registerData(AdultAlienEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> IS_CLIMBING = DataTracker.registerData(AdultAlienEntity.class,
			TrackedDataHandlerRegistry.BOOLEAN);

	private final SpiderNavigation landNavigation = new SpiderNavigation(this, world);
	private final AmphibiousNavigation swimNavigation = new AmphibiousNavigation(this, world);

	private final MoveControl landMoveControl = new MoveControl(this);
	private final LookControl landLookControl = new LookControl(this);
	private final AquaticMoveControl swimMoveControl = new AquaticMoveControl(this, 85, 10, 0.7f, 1.0f, false);
	private final YawAdjustingLookControl swimLookControl = new YawAdjustingLookControl(this, 10);

	private static final List<SensorType<? extends Sensor<? super LivingEntity>>> SENSOR_TYPES = List.of(
			SensorTypes.NEAREST_ALIEN_WEBBING, SensorType.NEAREST_LIVING_ENTITIES, SensorTypes.NEAREST_ALIEN_TARGET,
			SensorTypes.ALIEN_REPELLENT, SensorTypes.DESTRUCTIBLE_LIGHT);

	private static final List<MemoryModuleType<?>> MEMORY_MODULE_TYPES = List.of(MemoryModuleType.ATTACK_TARGET,
			MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
			MemoryModuleTypes.EGGMORPH_TARGET, MemoryModuleType.HOME, MemoryModuleType.LOOK_TARGET,
			MemoryModuleType.MOBS, MemoryModuleTypes.NEAREST_ALIEN_WEBBING, MemoryModuleType.NEAREST_ATTACKABLE,
			MemoryModuleTypes.NEAREST_LIGHT_SOURCE, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.PATH,
			MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.WALK_TARGET);

	public AdultAlienEntity(@NotNull EntityType<? extends AlienEntity> type, @NotNull World world) {
		super(type, world);
		stepHeight = 1.5f;

		navigation = landNavigation;
		moveControl = landMoveControl;
		lookControl = landLookControl;
	}

	public boolean isHissing() {
		return dataTracker.get(IS_HISSING);
	}

	public void setIsHissing(boolean isHissing) {
		dataTracker.set(IS_HISSING, isHissing);
	}

	public boolean isCrawling() {
		return dataTracker.get(IS_CLIMBING);
	}

	public void setIsCrawling(boolean isHissing) {
		dataTracker.set(IS_CLIMBING, isHissing);
	}

	private long hissingCooldown = 0L;

	public float getGrowth() {
		return dataTracker.get(GROWTH);
	}

	public void setGrowth(float growth) {
		dataTracker.set(GROWTH, growth);
	}

	private AdultAlienBrain complexBrain;

	@Override
	public Brain.Profile<? extends AdultAlienEntity> createBrainProfile() {
		return Brain.createProfile(MEMORY_MODULE_TYPES, SENSOR_TYPES);
	}

	@Override
	public Brain<? extends AdultAlienEntity> deserializeBrain(Dynamic<?> dynamic) {
		complexBrain = new AdultAlienBrain(this);
		Brain<? extends AdultAlienEntity> deserialize = createBrainProfile().deserialize(dynamic);
		return complexBrain.initialize(deserialize);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Brain<AdultAlienEntity> getBrain() {
		return (Brain<AdultAlienEntity>) super.getBrain();
	}

	@Override
	public void mobTick() {
		world.getProfiler().push("adultAlienBrain");
		complexBrain.tick();
		world.getProfiler().pop();
		complexBrain.tickActivities();
		super.mobTick();
	}

	@Override
	public void initDataTracker() {
		super.initDataTracker();
		dataTracker.startTracking(GROWTH, 0.0f);
		dataTracker.startTracking(IS_HISSING, false);
		dataTracker.startTracking(IS_CLIMBING, false);
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putFloat("growth", getGrowth());
		nbt.putBoolean("isHissing", isHissing());
		nbt.putBoolean("isCrawling", isCrawling());
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		if (nbt.contains("growth")) {
			setGrowth(nbt.getFloat("growth"));
		}
		if (nbt.contains("isHissing")) {
			setIsHissing(nbt.getBoolean("isHissing"));
		}
		if (nbt.contains("isCrawling")) {
			setIsCrawling(nbt.getBoolean("isCrawling"));
		}
	}

	@Override
	public int computeFallDamage(float fallDistance, float damageMultiplier) {
		if (fallDistance <= 15)
			return 0;
		return super.computeFallDamage(fallDistance, damageMultiplier);
	}

	@Override
	public int getSafeFallDistance() {
		return 9;
	}

	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
			EntityData entityData, NbtCompound entityNbt) {
		if (spawnReason != SpawnReason.NATURAL) {
			setGrowth(getMaxGrowth());
		}
		return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
	}

	@Override
	public void tick() {
		super.tick();

		if (!world.isClient && this.isAlive()) {
			grow(this, 1 * getGrowthMultiplier());
		}

		// Hissing Logic

		if (!world.isClient && isHissing()) {
			hissingCooldown = max(hissingCooldown - 1, 0);

			if (hissingCooldown <= 0) {
				setIsHissing(false);
			}
		}
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		var multiplier = 1.0f;
		if (source.isFire()) {
			multiplier = 2.0f;
		} else if (source.isProjectile()) {
			multiplier = 0.5f;
		}

		var isolationModeMultiplier = Gigeresque.config.features.isolationMode ? 0.05f : 1.0f;

		return super.damage(source, amount * multiplier * isolationModeMultiplier);
	}

	@Override
	public void onKilledOther(ServerWorld world, LivingEntity other) {
		super.onKilledOther(world, other);
		if (!world.isClient)
			this.heal(1.0833f);
	}

	@Override
	public boolean isClimbing() {
		boolean isAttacking = this.isAttacking();
		setIsCrawling(isAttacking && this.horizontalCollision && this.collides());
		return isAttacking && this.horizontalCollision && this.collides();
	}

	@Override
	protected void swimUpward(TagKey<Fluid> fluid) {
	}

	@Override
	public void travel(Vec3d movementInput) {
		this.navigation = (this.isSubmergedInWater() || this.isTouchingWater()) ? swimNavigation
				: this.isCrawling() ? landNavigation : landNavigation;
		this.moveControl = (this.submergedInWater || this.isTouchingWater()) ? swimMoveControl
				: this.isCrawling() ? landMoveControl : landMoveControl;
		this.lookControl = (this.submergedInWater || this.isTouchingWater()) ? swimLookControl : landLookControl;

		if (canMoveVoluntarily() && this.isTouchingWater()) {
			updateVelocity(getMovementSpeed(), movementInput);
			move(MovementType.SELF, getVelocity());
			setVelocity(getVelocity().multiply(0.9));
			if (getTarget() == null) {
				setVelocity(getVelocity().add(0.0, -0.005, 0.0));
			}
		} else {
			super.travel(movementInput);
		}
	}

	@Override
	public EntityNavigation createNavigation(World world) {
		return this.isTouchingWater() ? swimNavigation : this.isCrawling() ? landNavigation : landNavigation;
	}

	public boolean isCarryingEggmorphableTarget() {
		return !getPassengerList().isEmpty() && EntityUtils.isEggmorphable(this.getFirstPassenger());
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		if (!this.world.isClient) {
			complexBrain.stun(Constants.TPS * 3);
		}
		return super.updatePassengerForDismount(passenger);
	}

	/*
	 * GROWTH
	 */

	@Override
	public float getMaxGrowth() {
		return Constants.TPM;
	}

	@Override
	public LivingEntity growInto() {
		return null;
	}

	/*
	 * SOUNDS
	 */

	@Override
	public SoundEvent getAmbientSound() {
		return GigSounds.ALIEN_AMBIENT;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource source) {
		return GigSounds.ALIEN_HURT;
	}

	@Override
	public SoundEvent getDeathSound() {
		return GigSounds.ALIEN_DEATH;
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(5, new FleeFireGoal<AdultAlienEntity>(this));
	}

	@Override
	public void playAmbientSound() {
		if (!world.isClient) {
			setIsHissing(true);
			hissingCooldown = 80L;
		}
		super.playAmbientSound();
	}

	@Override
	public EntityDimensions getDimensions(EntityPose pose) {
		return this.submergedInWater || this.isInSneakingPose() && !this.isClimbing()
				? EntityDimensions.changing(1.0f, 1.0f)
				: super.getDimensions(pose);
	}

	@Override
	public int tickTimer() {
		return age;
	}
}
