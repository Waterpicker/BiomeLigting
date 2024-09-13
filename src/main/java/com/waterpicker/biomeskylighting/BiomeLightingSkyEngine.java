package com.waterpicker.biomeskylighting;

import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.core.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.SkyLightEngine;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.jetbrains.annotations.VisibleForTesting;

public class BiomeLightingSkyEngine extends SkyLightEngine {
    public final ChunkSkyLightSources emptyChunkSources;

    public BiomeLightingSkyEngine(LightChunkGetter pChunkSource) {
        this(pChunkSource, new SkyLightSectionStorage(pChunkSource));
    }

    @VisibleForTesting
    protected BiomeLightingSkyEngine(LightChunkGetter pChunkSource, SkyLightSectionStorage pSectionStorage) {
        super(pChunkSource, pSectionStorage);
        this.emptyChunkSources = new ChunkSkyLightSources(pChunkSource.getLevel());
    }

    public int getLowestSourceY(int pX, int pZ, int pDefaultReturnValue) {
        ChunkSkyLightSources chunkskylightsources = this.getChunkSources(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ));
        return chunkskylightsources == null ? pDefaultReturnValue : chunkskylightsources.getLowestSourceY(SectionPos.sectionRelative(pX), SectionPos.sectionRelative(pZ));
    }

    @Nullable
    public ChunkSkyLightSources getChunkSources(int pChunkX, int pChunkZ) {
        LightChunk lightchunk = chunkSource.getChunkForLighting(pChunkX, pChunkZ);
        return lightchunk != null ? lightchunk.getSkyLightSources() : null;
    }

    protected void checkNode(long pLevelPos) {
        int i = BlockPos.getX(pLevelPos);
        int j = BlockPos.getY(pLevelPos);
        int k = BlockPos.getZ(pLevelPos);
        long l = SectionPos.blockToSection(pLevelPos);
        int i1 = this.storage.lightOnInSection(l) ? this.getLowestSourceY(i, k, Integer.MAX_VALUE) : Integer.MAX_VALUE;
        if (i1 != Integer.MAX_VALUE) {
            this.updateSourcesInColumn(i, k, i1);
        }

        if (this.storage.storingLightForSection(l)) {
            boolean flag = j >= i1;
            if (flag) {
                this.enqueueDecrease(pLevelPos, LightEngine.QueueEntry.decreaseSkipOneDirection(getBiomeLightLevel(BlockPos.of(pLevelPos)), Direction.UP));
                this.enqueueIncrease(pLevelPos, LightEngine.QueueEntry.increaseSkipOneDirection(getBiomeLightLevel(BlockPos.of(pLevelPos)), false, Direction.UP));
            } else {
                int j1 = this.storage.getStoredLevel(pLevelPos);
                if (j1 > 0) {
                    this.storage.setStoredLevel(pLevelPos, 0);
                    this.enqueueDecrease(pLevelPos, LightEngine.QueueEntry.decreaseAllDirections(j1));
                } else {
                    this.enqueueDecrease(pLevelPos, PULL_LIGHT_IN_ENTRY);
                }
            }
        }
    }

    public void updateSourcesInColumn(int pX, int pZ, int pLowestY) {
        int i = SectionPos.sectionToBlockCoord(this.storage.getBottomSectionY());
        this.removeSourcesBelow(pX, pZ, pLowestY, i);
        this.addSourcesAbove(pX, pZ, pLowestY, i);
    }

    private void removeSourcesBelow(int pX, int pZ, int pMinY, int pBottomSectionY) {
        if (pMinY > pBottomSectionY) {
            int i = SectionPos.blockToSectionCoord(pX);
            int j = SectionPos.blockToSectionCoord(pZ);
            int k = pMinY - 1;

            for (int l = SectionPos.blockToSectionCoord(k); this.storage.hasLightDataAtOrBelow(l); --l) {
                if (this.storage.storingLightForSection(SectionPos.asLong(i, l, j))) {
                    int i1 = SectionPos.sectionToBlockCoord(l);
                    int j1 = i1 + 15;

                    for (int k1 = Math.min(j1, k); k1 >= i1; --k1) {
                        long l1 = BlockPos.asLong(pX, k1, pZ);
                        this.storage.setStoredLevel(l1, 0);
                        this.enqueueDecrease(l1, k1 == pMinY - 1 ? LightEngine.QueueEntry.decreaseAllDirections(getBiomeLightLevel(new BlockPos(i, l, k))) : LightEngine.QueueEntry.decreaseSkipOneDirection(getBiomeLightLevel(new BlockPos(i, l, k)), Direction.UP));
                    }
                }
            }

        }
    }

    public void addSourcesAbove(int pX, int pZ, int pMaxY, int pBottomSectionY) {
        int i = SectionPos.blockToSectionCoord(pX);
        int j = SectionPos.blockToSectionCoord(pZ);
        int k = Math.max(Math.max(this.getLowestSourceY(pX - 1, pZ, Integer.MIN_VALUE), this.getLowestSourceY(pX + 1, pZ, Integer.MIN_VALUE)), Math.max(this.getLowestSourceY(pX, pZ - 1, Integer.MIN_VALUE), this.getLowestSourceY(pX, pZ + 1, Integer.MIN_VALUE)));
        int l = Math.max(pMaxY, pBottomSectionY);

        for (long i1 = SectionPos.asLong(i, SectionPos.blockToSectionCoord(l), j); !this.storage.isAboveData(i1); i1 = SectionPos.offset(i1, Direction.UP)) {
            if (this.storage.storingLightForSection(i1)) {
                int j1 = SectionPos.sectionToBlockCoord(SectionPos.y(i1));
                int k1 = j1 + 15;

                for (int l1 = Math.max(j1, l); l1 <= k1; ++l1) {
                    long i2 = BlockPos.asLong(pX, l1, pZ);
                    this.storage.setStoredLevel(i2, getBiomeLightLevel(new BlockPos(pX, l, pZ)));
                    if (l1 < k || l1 == pMaxY) {
                        this.enqueueIncrease(i2, LightEngine.QueueEntry.increaseSkipOneDirection(getBiomeLightLevel(new BlockPos(pX, l, pZ)), false, Direction.UP));
                    }
                }
            }
        }
    }

    @Override
    public int countEmptySectionsBelowIfAtBorder(long pPackedPos) {
        int i = BlockPos.getY(pPackedPos);
        int j = SectionPos.sectionRelative(i);
        if (j != 0) {
            return 0;
        } else {
            int k = BlockPos.getX(pPackedPos);
            int l = BlockPos.getZ(pPackedPos);
            int i1 = SectionPos.sectionRelative(k);
            int j1 = SectionPos.sectionRelative(l);
            if (i1 != 0 && i1 != 15 && j1 != 0 && j1 != 15) {
                return 0;
            } else {
                int k1 = SectionPos.blockToSectionCoord(k);
                int l1 = SectionPos.blockToSectionCoord(i);
                int i2 = SectionPos.blockToSectionCoord(l);

                int j2;
                for (j2 = 0; !this.storage.storingLightForSection(SectionPos.asLong(k1, l1 - j2 - 1, i2)) && this.storage.hasLightDataAtOrBelow(l1 - j2 - 1); ++j2) {
                }

                return j2;
            }
        }
    }

    public void setLightEnabled(ChunkPos pChunkPos, boolean pLightEnabled) {
        super.setLightEnabled(pChunkPos, pLightEnabled);
        if (pLightEnabled) {
            ChunkSkyLightSources chunkskylightsources = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z), this.emptyChunkSources);
            int i = chunkskylightsources.getHighestLowestSourceY() - 1;
            int j = SectionPos.blockToSectionCoord(i) + 1;
            long k = SectionPos.getZeroNode(pChunkPos.x, pChunkPos.z);
            int l = this.storage.getTopSectionY(k);
            int i1 = Math.max(this.storage.getBottomSectionY(), j);

            for (int j1 = l - 1; j1 >= i1; --j1) {
                DataLayer datalayer = this.storage.getDataLayerToWrite(SectionPos.asLong(pChunkPos.x, j1, pChunkPos.z));
                if (datalayer != null && datalayer.isEmpty()) {
                    datalayer.fill(getBiomeLightLevel(pChunkPos.getWorldPosition()));
                }
            }
        }
    }

    private static boolean crossedSectionEdge(Direction pDirection, int pX, int pZ) {
        boolean flag;
        switch (pDirection) {
            case NORTH:
                flag = pZ == 15;
                break;
            case SOUTH:
                flag = pZ == 0;
                break;
            case WEST:
                flag = pX == 15;
                break;
            case EAST:
                flag = pX == 0;
                break;
            default:
                flag = false;
        }

        return flag;
    }

    private int getBiomeLightLevel(BlockPos pos) {
        ChunkAccess chunkAccess = (ChunkAccess) this.chunkSource.getChunkForLighting(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunkAccess != null) {
            Holder<Biome> biome = chunkAccess.getNoiseBiome(
                    QuartPos.fromBlock(pos.getX()),
                    QuartPos.fromBlock(pos.getY()),
                    QuartPos.fromBlock(pos.getZ())
            );

            // Use the biome light level or default to 15 if not available
            return BiomeMapLoader.getInstance().getLightValue(biome.unwrapKey().get().location()).orElse(15);
        } else {
            return 15;
        }
    }

    public void propagateLightSources(ChunkPos pChunkPos) {
        long sectionZeroNode = SectionPos.getZeroNode(pChunkPos.x, pChunkPos.z);
        this.storage.setLightEnabled(sectionZeroNode, true);

        // Retrieve the sky light sources for the current and neighboring chunks
        ChunkSkyLightSources centerSources = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z), this.emptyChunkSources);
        ChunkSkyLightSources northSources = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z - 1), this.emptyChunkSources);
        ChunkSkyLightSources southSources = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x, pChunkPos.z + 1), this.emptyChunkSources);
        ChunkSkyLightSources westSources = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x - 1, pChunkPos.z), this.emptyChunkSources);
        ChunkSkyLightSources eastSources = Objects.requireNonNullElse(this.getChunkSources(pChunkPos.x + 1, pChunkPos.z), this.emptyChunkSources);

        int topSectionY = this.storage.getTopSectionY(sectionZeroNode);
        int bottomSectionY = this.storage.getBottomSectionY();
        int chunkStartX = SectionPos.sectionToBlockCoord(pChunkPos.x);
        int chunkStartZ = SectionPos.sectionToBlockCoord(pChunkPos.z);

        // Iterate through all sections (Y levels)
        for (int sectionY = topSectionY - 1; sectionY >= bottomSectionY; --sectionY) {
            long sectionPos = SectionPos.asLong(pChunkPos.x, sectionY, pChunkPos.z);
            DataLayer dataLayer = this.storage.getDataLayerToWrite(sectionPos);

            if (dataLayer != null) {
                int blockStartY = SectionPos.sectionToBlockCoord(sectionY);
                int blockEndY = blockStartY + 15;
                boolean updated = false;

                // Iterate over the 16x16 blocks in the chunk section
                for (int blockX = 0; blockX < 16; ++blockX) {
                    for (int blockZ = 0; blockZ < 16; ++blockZ) {
                        // Get the lowest source of light for the current block
                        int lowestSourceY = centerSources.getLowestSourceY(blockX, blockZ);

                        if (lowestSourceY <= blockEndY) {
                            BlockPos blockPos = new BlockPos(chunkStartX + blockX, blockStartY, chunkStartZ + blockZ);
                            ChunkAccess chunkAccess = (ChunkAccess) this.chunkSource.getChunkForLighting(chunkStartX >> 4, chunkStartZ >> 4);

                            if (chunkAccess != null) {
                                // Fetch biome-specific light level
                                int biomeLightLevel = getBiomeLightLevel(blockPos);

                                // Get the lowest neighboring light levels
                                int northLight = blockZ == 0 ? northSources.getLowestSourceY(blockX, 15) : centerSources.getLowestSourceY(blockX, blockZ - 1);
                                int southLight = blockZ == 15 ? southSources.getLowestSourceY(blockX, 0) : centerSources.getLowestSourceY(blockX, blockZ + 1);
                                int westLight = blockX == 0 ? westSources.getLowestSourceY(15, blockZ) : centerSources.getLowestSourceY(blockX - 1, blockZ);
                                int eastLight = blockX == 15 ? eastSources.getLowestSourceY(0, blockZ) : centerSources.getLowestSourceY(blockX + 1, blockZ);
                                int maxNeighborLight = Math.max(Math.max(northLight, southLight), Math.max(westLight, eastLight));

                                // Propagate light upward through the chunk section
                                for (int blockY = blockEndY; blockY >= Math.max(blockStartY, lowestSourceY); --blockY) {
                                    // Set the biome-specific light level for the current block
                                    dataLayer.set(blockX, SectionPos.sectionRelative(blockY), blockZ, biomeLightLevel);

                                    // Check neighboring sections to propagate light across chunk borders
                                    if (blockY == lowestSourceY || blockY < maxNeighborLight) {
                                        long lightPos = BlockPos.asLong(chunkStartX + blockX, blockY, chunkStartZ + blockZ);
                                        this.enqueueIncrease(lightPos, increaseSkySourceInDirections(
                                                blockY == lowestSourceY, // Down
                                                blockY < northLight,     // North
                                                blockY < southLight,     // South
                                                blockY < westLight,      // West
                                                blockY < eastLight,      // East
                                                biomeLightLevel
                                        ));
                                    }
                                }

                                // If light was updated in this section, mark it for further updates
                                if (lowestSourceY < blockStartY) {
                                    updated = true;
                                }
                            }
                        }
                    }
                }

                // If no updates are needed for this section, we can stop propagating further
                if (!updated) {
                    break;
                }
            }
        }
    }

    public long increaseSkySourceInDirections(boolean pDown, boolean pNorth, boolean pSouth, boolean pWest, boolean pEast, int currentLightLevel) {
        // Ensure light falls off by reducing the light level by 1 for each step
        long entry = withLevel(0L, currentLightLevel);

        // Set the propagation flags for each direction, assuming we're propagating the reduced light
        if (pDown) {
            entry = withDirection(entry, Direction.DOWN);
        }
        if (pNorth) {
            entry = withDirection(entry, Direction.NORTH);
        }
        if (pSouth) {
            entry = withDirection(entry, Direction.SOUTH);
        }
        if (pWest) {
            entry = withDirection(entry, Direction.WEST);
        }
        if (pEast) {
            entry = withDirection(entry, Direction.EAST);
        }

        return entry;
    }

    private static long withLevel(long pEntry, int pLevel) {
        return pEntry & -16L | (long) pLevel;
    }

    private static long withDirection(long pEntry, Direction pDirection) {
        return pEntry | 1L << pDirection.ordinal() + 4;
    }
}