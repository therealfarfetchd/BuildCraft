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

package buildcraft.lib.misc;

import buildcraft.api.core.BCLog;
import buildcraft.lib.net.PacketBufferBC;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SpecialHandler {
    private SpecialHandler() {
    }

    private static final Map<Class<? extends TileEntity>, SpecialPayloadHandler> handlerOverrides = new HashMap<>();
    private static final Map<Class<? extends IBlockAccess>, Function<IBlockAccess, Optional<WorldServer>>> worldExtractors = new HashMap<>();

    public static boolean hasOverrideForTile(TileEntity t) {
        return handlerOverrides.containsKey(t.getClass());
    }

    public static IMessage handleTileMessage(TileEntity t, BlockPos pos, PacketBufferBC payload, MessageContext ctx) throws IOException {
        if (!hasOverrideForTile(t))
            return null;
        return handlerOverrides.get(t.getClass()).receivePayload(pos, payload, ctx);
    }

    public static void addPayloadHandler(Class<? extends TileEntity> teClass, SpecialPayloadHandler handler) {
        if (handlerOverrides.containsKey(teClass)) {
            BCLog.logger.warn("Overriding existing special handler for {}! This might break things.", teClass);
        }
        BCLog.logger.info("Registered special handler for {}.", teClass);
        handlerOverrides.put(teClass, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IBlockAccess> void addWorldExtractor(Class<T> worldClass, Function<T, Optional<WorldServer>> transform) {
        worldExtractors.put(worldClass, (Function<IBlockAccess, Optional<WorldServer>>) transform);
    }

    public static Optional<WorldServer> extractWorld(IBlockAccess world) {
        if (worldExtractors.containsKey(world.getClass())) {
            return worldExtractors.get(world.getClass()).apply(world);
        }
        return Optional.empty();
    }

    static {
        addWorldExtractor(WorldServer.class, Optional::of);
    }

    @FunctionalInterface
    public static interface SpecialPayloadHandler {
        public IMessage receivePayload(BlockPos pos, PacketBufferBC payload, MessageContext ctx) throws IOException;
    }
}
