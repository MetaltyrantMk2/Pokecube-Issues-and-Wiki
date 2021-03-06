package thut.api.terrain;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.world.gen.feature.structure.StructurePiece;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class StructureManager
{
    public static class StructureInfo
    {
        public String         name;
        public StructureStart start;

        public StructureInfo()
        {
        }

        public StructureInfo(final Entry<String, StructureStart> entry)
        {
            this.name = entry.getKey();
            this.start = entry.getValue();
        }

        public boolean isIn(final BlockPos pos)
        {
            if (!this.start.getBoundingBox().isVecInside(pos)) return false;
            for (final StructurePiece p1 : this.start.getComponents())
                if (this.isIn(p1.getBoundingBox(), pos)) return true;
            return false;
        }

        private boolean isIn(final MutableBoundingBox b, BlockPos pos)
        {
            final int x1 = pos.getX();
            final int y1 = pos.getY();
            final int z1 = pos.getZ();
            for (int x = x1; x < x1 + TerrainSegment.GRIDSIZE; x++)
                for (int y = y1; y < y1 + TerrainSegment.GRIDSIZE; y++)
                    for (int z = z1; z < z1 + TerrainSegment.GRIDSIZE; z++)
                    {
                        pos = new BlockPos(x, y, z);
                        if (b.isVecInside(pos)) return true;
                    }
            return false;
        }

        @Override
        public int hashCode()
        {
            return this.name.hashCode() + this.start.getBoundingBox().toString().hashCode();
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (!(obj instanceof StructureInfo)) return false;
            final StructureInfo other = (StructureInfo) obj;
            if (!other.name.equals(this.name)) return false;
            return StructureInfo.sameBounds(other.start.getBoundingBox(), this.start.getBoundingBox());
        }

        @Override
        public String toString()
        {
            return this.name + " " + this.start.getBoundingBox();
        }

        private static boolean sameBounds(final MutableBoundingBox boxA, final MutableBoundingBox boxB)
        {
            return boxA.maxX == boxB.maxX && boxA.maxY == boxB.maxY && boxA.maxZ == boxB.maxX && boxA.minX == boxB.minX
                    && boxA.minY == boxB.minY && boxA.minZ == boxB.minX;
        }
    }

    private static Long2ObjectOpenHashMap<Set<StructureInfo>> map_by_chunk = new Long2ObjectOpenHashMap<>();

    private static Set<StructureInfo> getOrMake(final long pos)
    {
        final Set<StructureInfo> set = StructureManager.map_by_chunk.getOrDefault(pos, Sets.newHashSet());
        if (!StructureManager.map_by_chunk.containsKey(pos)) StructureManager.map_by_chunk.put(pos, set);
        return set;
    }

    public static Set<StructureInfo> getFor(final long pos)
    {
        return StructureManager.map_by_chunk.getOrDefault(pos, Collections.emptySet());
    }

    public static Set<StructureInfo> getFor(final BlockPos loc)
    {
        final Set<StructureInfo> forPos = StructureManager.getFor(new ChunkPos(loc).asLong());
        if (forPos.isEmpty()) return forPos;
        final Set<StructureInfo> matches = Sets.newHashSet();
        for (final StructureInfo i : forPos)
            if (i.isIn(loc)) matches.add(i);
        return matches;
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load evt)
    {
        for (final Entry<String, StructureStart> entry : evt.getChunk().getStructureStarts().entrySet())
        {
            final StructureInfo info = new StructureInfo(entry);
            final MutableBoundingBox b = info.start.getBoundingBox();
            for (int x = b.minX >> 4; x <= b.maxX >> 4; x++)
                for (int z = b.minZ >> 4; z <= b.maxZ >> 4; z++)
                {
                    final ChunkPos p = new ChunkPos(x, z);
                    final Set<StructureInfo> set = StructureManager.getOrMake(p.asLong());
                    set.add(info);
                }
        }
    }

    public static void clear()
    {
        StructureManager.map_by_chunk.clear();
    }
}
