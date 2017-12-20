/*
 * Copyright (c) 2017 Marco Rebhan (the_real_farfetchd)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package buildcraft.compat.mcmultipart;

import buildcraft.transport.BCTransportBlocks;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.slot.EnumCenterSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

public class PartPipeHolder implements IMultipart {
    public static final PartPipeHolder INSTANCE = new PartPipeHolder();

    private PartPipeHolder() {
    }

    @Override
    public Block getBlock() {
        return BCTransportBlocks.pipeHolder;
    }

    @Override
    public IPartSlot getSlotForPlacement(World world, BlockPos blockPos, IBlockState iBlockState, EnumFacing enumFacing, float v, float v1, float v2, EntityLivingBase entityLivingBase) {
        return EnumCenterSlot.CENTER;
    }

    @Override
    public IPartSlot getSlotFromWorld(IBlockAccess iBlockAccess, BlockPos blockPos, IBlockState iBlockState) {
        return EnumCenterSlot.CENTER;
    }

    @Override
    public List<AxisAlignedBB> getOcclusionBoxes(IPartInfo part) {
        return Collections.emptyList();
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IPartInfo part) {
        // unbox the hit result since pipe's getSelectedBoundingBox goes nuts otherwise
        Minecraft mc = Minecraft.getMinecraft();
        RayTraceResult backup = mc.objectMouseOver;
        if (backup.hitInfo instanceof RayTraceResult) {
            mc.objectMouseOver = (RayTraceResult) backup.hitInfo;
            AxisAlignedBB result = part.getState().getSelectedBoundingBox(part.getPartWorld(), part.getPartPos());
            mc.objectMouseOver = backup;
            return result;
        }
        return Block.FULL_BLOCK_AABB;
    }

    @Override
    public boolean canPlayerDestroy(IPartInfo part, EntityPlayer player) {
        return BCTransportBlocks.pipeHolder.removePipeParts(part.getState(), part.getPartWorld(), part.getPartPos(), player);
    }
}
