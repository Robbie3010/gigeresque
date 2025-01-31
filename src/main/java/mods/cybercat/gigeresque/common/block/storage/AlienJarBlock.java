package mods.cybercat.gigeresque.common.block.storage;

import java.util.stream.Stream;

import mods.cybercat.gigeresque.common.block.StorageProperties;
import mods.cybercat.gigeresque.common.block.StorageStates;
import mods.cybercat.gigeresque.common.block.entity.JarStorageEntity;
import mods.cybercat.gigeresque.common.block.material.Materials;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class AlienJarBlock extends BlockWithEntity {

	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final EnumProperty<StorageStates> STORAGE_STATE = StorageProperties.STORAGE_STATE;
	private static final VoxelShape OUTLINE_SHAPE = Stream.of(Block.createCuboidShape(5.5, 0, 5.5, 10.5, 3, 10.5),
			Block.createCuboidShape(5, 3, 5, 11, 9, 11), Block.createCuboidShape(4.5, 9, 4.5, 11.5, 16, 11.5),
			Block.createCuboidShape(4.5, 12, 4.5, 11.5, 16, 11.5), Block.createCuboidShape(5, 16, 5, 11, 18, 11))
			.reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();

	public AlienJarBlock() {
		super(FabricBlockSettings.of(Materials.ORGANIC_ALIEN_BLOCK).sounds(BlockSoundGroup.DRIPSTONE_BLOCK)
				.strength(5.0f, 8.0f).nonOpaque());
		this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(STORAGE_STATE,
				StorageStates.CLOSED));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, STORAGE_STATE);
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation((Direction) state.get(FACING)));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return this.getDefaultState().with(FACING, context.getPlayerFacing());
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
			BlockHitResult hit) {
		if (!world.isClient && world.getBlockEntity(pos)instanceof JarStorageEntity jarStorageEntity)
			player.openHandledScreen(jarStorageEntity);
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new JarStorageEntity(pos, state);
	}

	@Override
	public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
		return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE_SHAPE;
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos)instanceof JarStorageEntity jarStorageEntity)
			jarStorageEntity.tick();
	}
}
