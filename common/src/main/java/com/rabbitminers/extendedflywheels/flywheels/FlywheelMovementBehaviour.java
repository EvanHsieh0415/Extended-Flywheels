package com.rabbitminers.extendedflywheels.flywheels;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.rabbitminers.extendedflywheels.base.FlywheelRotationType;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.content.contraptions.components.structureMovement.TranslatingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.render.ContraptionMatrices;
import com.simibubi.create.content.logistics.trains.entity.CarriageBogey;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraption;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class FlywheelMovementBehaviour implements MovementBehaviour {
    @Override
    public boolean renderAsNormalTileEntity() {
        return true;
    }

    @Nullable
    private IRotatingTileEnity getTileEntity(MovementContext context) {
        Map<BlockPos, BlockEntity> tes = context.contraption.presentTileEntities;
        if (!(tes.get(context.localPos) instanceof IRotatingTileEnity te))
            return null;
        return te;
    }

    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (!context.world.isClientSide)
            return;
        Map<BlockPos, BlockEntity> tileEntities = context.contraption.presentTileEntities;
        if (!(tileEntities.get(context.localPos) instanceof IRotatingTileEnity te))
            return;
        if (Minecraft.getInstance().isPaused())
            return;
        if (!(context.contraption instanceof CarriageContraption carriageContraption))
            return;

        CompoundTag nbt = ((BlockEntity) te).getUpdateTag();
        FlywheelRotationType rotationType =
                FlywheelRotationType.values()[nbt.getInt("ScrollValue")];
        if (rotationType == FlywheelRotationType.STATIONARY)
            return;

        Direction direction = carriageContraption.getAssemblyDirection();
        double distanceTo = 0;

        if (carriageContraption.entity instanceof CarriageContraptionEntity cce && !cce.firstPositionUpdate) {
            Vec3 diff = context.motion;
            Vec3 relativeDiff = VecHelper.rotate(diff, cce.yaw, Direction.Axis.Y);
            double signum = Math.signum(-relativeDiff.x);
            distanceTo = diff.length() * signum/3f;
        }

        double wheelRadius = te.getWheelRadius();
        double angleDiff = 360 * distanceTo / (Math.PI * 2 * wheelRadius);

        if (rotationType == FlywheelRotationType.TRAIL)
            direction = direction.getOpposite();

        switch (direction) {
            case NORTH, EAST -> te.setAngle((float) ((te.getAngle() + (angleDiff * 3 / 10f)) % 360));
            case SOUTH, WEST -> te.setAngle((float) ((te.getAngle() - (angleDiff * 3 / 10f)) % 360));
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (!context.world.isClientSide || !isActive(context))
            return;
        IRotatingTileEnity te = getTileEntity(context);
        if (te != null)
            te.unsetForcedSpeed();
    }
}
