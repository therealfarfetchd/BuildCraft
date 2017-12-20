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

package buildcraft.transport.pipe;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import java.util.*;
import java.util.function.Function;

public class SpecialHandlerPipe {
    private static final Map<Class<? extends TileEntity>, Function<TileEntity, Optional<TilePipeHolder>>> pipeExtractors = new HashMap<>();
    private static final Set<PipeConnectionLogic> conditions = new HashSet<>();

    static {
        addPipeExtractor(TilePipeHolder.class, Optional::of);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TileEntity> void addPipeExtractor(Class<T> worldClass, Function<? super T, Optional<TilePipeHolder>> transform) {
        pipeExtractors.put(worldClass, (Function<TileEntity, Optional<TilePipeHolder>>) transform);
    }

    public static Optional<TilePipeHolder> extractPipe(TileEntity tile) {
        if (tile != null && pipeExtractors.containsKey(tile.getClass())) {
            return pipeExtractors.get(tile.getClass()).apply(tile);
        }
        return Optional.empty();
    }

    public static boolean canPipesConnect(EnumFacing facing, IPipe self, IPipe other) {
        return checkPipe(facing, self) && checkPipe(facing.getOpposite(), other);
    }

    private static boolean checkPipe(EnumFacing facing, IPipe self) {
        return conditions.stream().allMatch(it -> it.checkPipe(facing, self));
    }

    public static void addConnectionCondition(PipeConnectionLogic c) {
        conditions.add(c);
    }

    @FunctionalInterface
    public static interface PipeConnectionLogic {
        public boolean checkPipe(EnumFacing facing, IPipe self);
    }

    private SpecialHandlerPipe() {
    }
}
