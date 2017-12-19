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

import buildcraft.api.core.BCLog;
import buildcraft.core.BCCore;
import buildcraft.lib.BCLibProxy;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.net.IPayloadReceiver;
import buildcraft.lib.net.MessageUpdateTile;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.tile.TilePipeHolder;
import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.ref.MCMPCapabilities;
import mcmultipart.block.TileMultipartContainer;
import mcmultipart.util.MCMPWorldWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@MCMPAddon
// TODO: move to BCCompat
public class MultipartAddon implements IMCMPAddon {
    public MultipartAddon() {
        MinecraftForge.EVENT_BUS.register(this);

        MessageUpdateTile.SpecialHandler specialHandler = (pos, payload, ctx) -> {
            EntityPlayer player = BCLibProxy.getProxy().getPlayerForContext(ctx);
            if (player == null || player.world == null) return null;
            TileEntity tile = player.world.getTileEntity(pos);
            if (tile instanceof TileMultipartContainer) {
                TileMultipartContainer tmc = (TileMultipartContainer) tile;
                Collection<IPayloadReceiver> result = tmc.getParts().values().stream()
                        .filter(it -> it.getTile() != null)
                        .filter(it -> it.getTile().getTileEntity() instanceof IPayloadReceiver)
                        .map(it -> (IPayloadReceiver) it.getTile().getTileEntity())
                        .collect(Collectors.toSet());
                if (result.size() == 1) {
                    return result.iterator().next().receivePayload(ctx, payload);
                } else {
                    BCLog.logger.error("Ambiguous target at position {}: TileEntity with IPayloadReceiver", pos);
                }
            }
            return null;
        };
        MessageUpdateTile.addSpecialHandler(TileMultipartContainer.class, specialHandler);
        MessageUpdateTile.addSpecialHandler(TileMultipartContainer.Ticking.class, specialHandler);

        MessageUtil.addWorldExtractor(MCMPWorldWrapper.class, it -> {
            World w = it.getActualWorld();
            if (w instanceof WorldServer) {
                return Optional.of((WorldServer) w);
            }
            return Optional.empty();
        });
    }

    @Override
    public void registerParts(IMultipartRegistry registry) {
        registry.registerPartWrapper(BCTransportBlocks.pipeHolder, PartPipeHolder.INSTANCE);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> e) {
        System.out.println(e.getObject());
        if (e.getObject() instanceof TilePipeHolder) {
            TilePartPipeHolder tpph = new TilePartPipeHolder((TilePipeHolder) e.getObject());
            e.addCapability(new ResourceLocation(BCCore.MODID, "multipart"), new ICapabilityProvider() {
                @Override
                public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                    return getCapability(capability, facing) != null;
                }

                @SuppressWarnings("unchecked")
                @Nullable
                @Override
                public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                    if (capability == MCMPCapabilities.MULTIPART_TILE)
                        return (T) tpph;

                    return null;
                }
            });
        }
    }
}
