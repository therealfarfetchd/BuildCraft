/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import buildcraft.api.core.BCLog;
import buildcraft.lib.BCLibProxy;
import buildcraft.lib.misc.MessageUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageUpdateTile implements IMessage {
    private static Map<Class<? extends TileEntity>, SpecialHandler> handlerOverrides = new HashMap<>();

    private BlockPos pos;
    private PacketBufferBC payload;

    @SuppressWarnings("unused")
    public MessageUpdateTile() {
    }

    public MessageUpdateTile(BlockPos pos, PacketBufferBC payload) {
        this.pos = pos;
        this.payload = payload;
        if (getPayloadSize() > 1 << 24) {
            throw new IllegalStateException("Can't write out " + getPayloadSize() + "bytes!");
        }
    }

    public int getPayloadSize() {
        return payload == null ? 0 : payload.readableBytes();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = MessageUtil.readBlockPos(new PacketBuffer(buf));
        int size = buf.readUnsignedMedium();
        payload = new PacketBufferBC(buf.readBytes(size));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        MessageUtil.writeBlockPos(new PacketBuffer(buf), pos);
        int length = payload.readableBytes();
        buf.writeMedium(length);
        buf.writeBytes(payload, 0, length);
    }

    public static final IMessageHandler<MessageUpdateTile, IMessage> HANDLER = (message, ctx) -> {
        EntityPlayer player = BCLibProxy.getProxy().getPlayerForContext(ctx);
        if (player == null || player.world == null) {
            return null;
        }
        TileEntity tile = player.world.getTileEntity(message.pos);

        try {
            if (tile != null && handlerOverrides.containsKey(tile.getClass())) {
                return handlerOverrides.get(tile.getClass()).receivePayload(message.pos, message.payload, ctx);
            } else if (tile instanceof IPayloadReceiver) {
                return ((IPayloadReceiver) tile).receivePayload(ctx, message.payload);
            } else {
                BCLog.logger.warn("Dropped message for player " + player.getName() + " for tile at " + message.pos
                        + " (found " + tile + ")");
            }
        } catch (IOException io) {
            throw new RuntimeException(io);
        }

        return null;
    };

    public static <T extends TileEntity> void addSpecialHandler(Class<T> teClass, SpecialHandler handler) {
        if (handlerOverrides.containsKey(teClass)) {
            BCLog.logger.warn("Overriding existing special handler for {}! This might break things.", teClass);
        }
        BCLog.logger.info("Registered special handler for {}.", teClass);
        handlerOverrides.put(teClass, handler);
    }

    @FunctionalInterface
    public static interface SpecialHandler {
        public IMessage receivePayload(BlockPos pos, PacketBufferBC payload, MessageContext ctx) throws IOException;
    }
}
