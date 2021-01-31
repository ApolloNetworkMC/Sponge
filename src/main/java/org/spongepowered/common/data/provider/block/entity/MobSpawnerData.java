/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.data.provider.block.entity;

import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityArchetype;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.util.weighted.TableEntry;
import org.spongepowered.api.util.weighted.WeightedObject;
import org.spongepowered.api.util.weighted.WeightedSerializableObject;
import org.spongepowered.api.util.weighted.WeightedTable;
import org.spongepowered.common.accessor.util.WeighedRandom_WeighedRandomItemAccessor;
import org.spongepowered.common.accessor.world.level.BaseSpawnerAccessor;
import org.spongepowered.common.accessor.world.level.block.entity.SpawnerBlockEntityAccessor;
import org.spongepowered.common.data.persistence.NBTTranslator;
import org.spongepowered.common.data.provider.DataProviderRegistrator;
import org.spongepowered.common.util.Constants;
import org.spongepowered.common.util.SpongeTicks;

public final class MobSpawnerData {

    private MobSpawnerData() {
    }

    // @formatter:off
    public static void register(final DataProviderRegistrator registrator) {
        registrator
                .asMutable(SpawnerBlockEntityAccessor.class)
                    .create(Keys.MAX_NEARBY_ENTITIES)
                        .get(h -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$maxNearbyEntities())
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$maxNearbyEntities(v))
                    .create(Keys.MAX_SPAWN_DELAY)
                        .get(h -> new SpongeTicks(((BaseSpawnerAccessor) h.accessor$spawner()).accessor$maxSpawnDelay()))
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$maxSpawnDelay((int) v.getTicks()))
                    .create(Keys.MIN_SPAWN_DELAY)
                        .get(h -> new SpongeTicks(((BaseSpawnerAccessor) h.accessor$spawner()).accessor$minSpawnDelay()))
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$minSpawnDelay((int) v.getTicks()))
                    .create(Keys.NEXT_ENTITY_TO_SPAWN)
                        .get(h -> MobSpawnerData.getNextEntity((BaseSpawnerAccessor) h.accessor$spawner()))
                        .set((h, v) -> MobSpawnerData.setNextEntity((SpawnerBlockEntity) h, v))
                    .create(Keys.REMAINING_SPAWN_DELAY)
                        .get(h -> new SpongeTicks(((BaseSpawnerAccessor) h.accessor$spawner()).accessor$spawnDelay()))
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$spawnDelay((int) v.getTicks()))
                    .create(Keys.REQUIRED_PLAYER_RANGE)
                        .get(h -> (double) ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$requiredPlayerRange())
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$requiredPlayerRange(v.intValue()))
                    .create(Keys.SPAWN_COUNT)
                        .get(h -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$spawnCount())
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$spawnCount(v))
                    .create(Keys.SPAWN_RANGE)
                        .get(h -> (double) ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$spawnRange())
                        .set((h, v) -> ((BaseSpawnerAccessor) h.accessor$spawner()).accessor$spawnRange(v.intValue()))
                    .create(Keys.SPAWNABLE_ENTITIES)
                        .get(h -> MobSpawnerData.getEntities(h.accessor$spawner()))
                        .set((h, v) -> {
                            final BaseSpawnerAccessor logic = (BaseSpawnerAccessor) h.accessor$spawner();
                            MobSpawnerData.setEntities(logic, v);
                            MobSpawnerData.setNextEntity((SpawnerBlockEntity) h, MobSpawnerData.getNextEntity(logic));
                        });
    }
    // @formatter:on

    private static WeightedSerializableObject<EntityArchetype> getNextEntity(final BaseSpawnerAccessor logic) {
        final int weight = ((WeighedRandom_WeighedRandomItemAccessor) logic.accessor$nextSpawnData()).accessor$weight();

        final String resourceLocation = logic.accessor$nextSpawnData().getTag().getString(Constants.Entity.ENTITY_TYPE_ID);
        final EntityType<?> type =
                Registry.ENTITY_TYPE.getOptional(new ResourceLocation(resourceLocation)).map(EntityType.class::cast).orElse(EntityTypes.PIG.get());

        final CompoundTag data = logic.accessor$nextSpawnData().getTag();

        final EntityArchetype archetype = EntityArchetype.builder()
                .type(type)
                .entityData(NBTTranslator.INSTANCE.translateFrom(data))
                .build();

        return new WeightedSerializableObject<>(archetype, weight);
    }

    private static void setNextEntity(final SpawnerBlockEntity entity, final WeightedSerializableObject<EntityArchetype> value) {
        final CompoundTag compound = NBTTranslator.INSTANCE.translate(value.get().getEntityData());
        if (!compound.contains(Constants.Entity.ENTITY_TYPE_ID)) {
            final ResourceKey key = (ResourceKey) (Object) net.minecraft.world.entity.EntityType.getKey((net.minecraft.world.entity.EntityType<?>) value.get()
                    .getType());
            compound.putString(Constants.Entity.ENTITY_TYPE_ID, key.toString());
        }

        entity.getSpawner().setNextSpawnData(entity.getLevel(), entity.getBlockPos(), new SpawnData((int) value.getWeight(), compound));
    }

    private static WeightedTable<EntityArchetype> getEntities(final BaseSpawner logic) {
        final WeightedTable<EntityArchetype> possibleEntities = new WeightedTable<>();
        for (final SpawnData weightedEntity : ((BaseSpawnerAccessor) logic).accessor$spawnPotentials()) {

            final CompoundTag nbt = weightedEntity.getTag();

            final String resourceLocation = nbt.getString(Constants.Entity.ENTITY_TYPE_ID);
            final EntityType<?> type =
                    Registry.ENTITY_TYPE.getOptional(new ResourceLocation(resourceLocation)).map(EntityType.class::cast).orElse(EntityTypes.PIG.get());

            final EntityArchetype archetype = EntityArchetype.builder()
                    .type(type)
                    .entityData(NBTTranslator.INSTANCE.translateFrom(nbt))
                    .build();

            possibleEntities
                    .add(new WeightedSerializableObject<>(archetype, ((WeighedRandom_WeighedRandomItemAccessor) weightedEntity).accessor$weight()));
        }

        return possibleEntities;
    }

    private static void setEntities(final BaseSpawnerAccessor logic, final WeightedTable<EntityArchetype> table) {
        logic.accessor$spawnPotentials().clear();
        for (final TableEntry<EntityArchetype> entry : table) {
            if (!(entry instanceof WeightedObject)) {
                continue;
            }
            final WeightedObject<EntityArchetype> object = (WeightedObject<EntityArchetype>) entry;

            final CompoundTag compound = NBTTranslator.INSTANCE.translate(object.get().getEntityData());
            if (!compound.contains(Constants.Entity.ENTITY_TYPE_ID)) {
                final ResourceKey key = (ResourceKey) (Object) net.minecraft.world.entity.EntityType.getKey((net.minecraft.world.entity.EntityType<?>) object
                        .get().getType());
                compound.putString(Constants.Entity.ENTITY_TYPE_ID, key.toString());
            }


            logic.accessor$spawnPotentials().add(new SpawnData((int) entry.getWeight(), compound));
        }
    }
}
