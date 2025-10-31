package com.poorcraft.ultra.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jme3.math.Vector3f;
import com.poorcraft.ultra.gameplay.Inventory;
import com.poorcraft.ultra.gameplay.ItemStack;
import com.poorcraft.ultra.player.BlockHitResult;
import com.poorcraft.ultra.player.BlockPicker;
import com.poorcraft.ultra.voxel.BlockType;
import com.poorcraft.ultra.voxel.Chunk;
import com.poorcraft.ultra.voxel.ChunkPos;
import com.poorcraft.ultra.voxel.ChunkStorage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class Phase12Tests {

    @Nested
    class ItemStackTests {

        @Test
        void nullBlockTypeThrows() {
            assertThatThrownBy(() -> ItemStack.of(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void invalidCountThrows() {
            assertThatThrownBy(() -> ItemStack.of(BlockType.STONE, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ItemStack.of(BlockType.DIRT, Inventory.MAX_STACK_SIZE + 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void incrementAndDecrementRespectBoundsAndImmutability() {
            ItemStack stack = ItemStack.of(BlockType.GRASS, 10);
            ItemStack incremented = stack.increment(5);
            ItemStack capped = stack.increment(Inventory.MAX_STACK_SIZE);
            ItemStack decremented = stack.decrement(50);

            assertThat(stack.count()).isEqualTo(10);
            assertThat(incremented.count()).isEqualTo(15);
            assertThat(capped.count()).isEqualTo(Inventory.MAX_STACK_SIZE);
            assertThat(decremented.count()).isEqualTo(1);
            assertThat(incremented).isNotSameAs(stack);
            assertThat(decremented).isNotSameAs(stack);
        }

        @Test
        void canStackWithMatchesBlockType() {
            ItemStack stone = ItemStack.of(BlockType.STONE, 5);
            ItemStack stoneFull = ItemStack.of(BlockType.STONE, Inventory.MAX_STACK_SIZE);
            ItemStack dirt = ItemStack.of(BlockType.DIRT, 3);

            assertThat(stone.canStackWith(stoneFull)).isTrue();
            assertThat(stone.canStackWith(dirt)).isFalse();
            assertThat(stoneFull.isFull()).isTrue();
        }
    }

    @Nested
    class InventoryTests {

        @Test
        void addItemMergesIntoExistingStackAndNotifies() {
            Inventory inventory = new Inventory();
            inventory.setSlot(0, ItemStack.of(BlockType.STONE, 10));

            AtomicInteger notifications = new AtomicInteger();
            inventory.addListener(inv -> notifications.incrementAndGet());
            notifications.set(0);

            boolean success = inventory.addItem(BlockType.STONE, 20);

            assertThat(success).isTrue();
            ItemStack merged = inventory.getSlot(0);
            assertThat(merged).isNotNull();
            assertThat(merged.count()).isEqualTo(30);
            assertThat(notifications.get()).isEqualTo(1);
        }

        @Test
        void partialAddStillNotifiesListeners() {
            Inventory inventory = new Inventory();
            inventory.setSlot(0, ItemStack.of(BlockType.STONE, 60));
            for (int i = 1; i < Inventory.TOTAL_SIZE; i++) {
                inventory.setSlot(i, ItemStack.of(BlockType.DIRT, Inventory.MAX_STACK_SIZE));
            }

            AtomicInteger notifications = new AtomicInteger();
            inventory.addListener(inv -> notifications.incrementAndGet());
            notifications.set(0);

            boolean success = inventory.addItem(BlockType.STONE, 10);

            assertThat(success).isFalse();
            ItemStack slot = inventory.getSlot(0);
            assertThat(slot).isNotNull();
            assertThat(slot.count()).isEqualTo(Inventory.MAX_STACK_SIZE);
            assertThat(notifications.get()).isEqualTo(1);
        }

        @Test
        void removeFromSlotRespectsSlotBoundaries() {
            Inventory inventory = new Inventory();
            inventory.setSlot(0, ItemStack.of(BlockType.STONE, 3));

            AtomicInteger notifications = new AtomicInteger();
            inventory.addListener(inv -> notifications.incrementAndGet());
            notifications.set(0);

            boolean result = inventory.removeFromSlot(0, 2);

            assertThat(result).isTrue();
            ItemStack remaining = inventory.getSlot(0);
            assertThat(remaining).isNotNull();
            assertThat(remaining.count()).isEqualTo(1);
            assertThat(notifications.get()).isEqualTo(1);

            boolean failed = inventory.removeFromSlot(0, 5);
            assertThat(failed).isFalse();
            ItemStack afterFailure = inventory.getSlot(0);
            assertThat(afterFailure).isNotNull();
            assertThat(afterFailure.count()).isEqualTo(1);
            assertThat(notifications.get()).isEqualTo(1);
        }

        @Test
        void removeItemAggregatesAcrossSlots() {
            Inventory inventory = new Inventory();
            inventory.setSlot(0, ItemStack.of(BlockType.STONE, 3));
            inventory.setSlot(1, ItemStack.of(BlockType.STONE, 2));

            AtomicInteger notifications = new AtomicInteger();
            inventory.addListener(inv -> notifications.incrementAndGet());
            notifications.set(0);

            boolean removed = inventory.removeItem(BlockType.STONE, 4);

            assertThat(removed).isTrue();
            assertThat(inventory.getSlot(0)).isNull();
            ItemStack remaining = inventory.getSlot(1);
            assertThat(remaining).isNotNull();
            assertThat(remaining.count()).isEqualTo(1);
            assertThat(notifications.get()).isEqualTo(1);
        }

        @Test
        void setSelectedSlotNotifiesListenersOnChange() {
            Inventory inventory = new Inventory();

            AtomicInteger notifications = new AtomicInteger();
            inventory.addListener(inv -> notifications.incrementAndGet());

            inventory.setSelectedSlot(3);
            assertThat(inventory.getSelectedSlot()).isEqualTo(3);
            assertThat(notifications.get()).isEqualTo(1);

            inventory.setSelectedSlot(3);
            assertThat(notifications.get()).isEqualTo(1);

            assertThatThrownBy(() -> inventory.setSelectedSlot(-1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> inventory.setSelectedSlot(Inventory.HOTBAR_SIZE))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class BlockMathTests {

        @Test
        void blockHitResultProvidesPlacementCoordinates() {
            Vector3f blockPos = new Vector3f(5f, 10f, 15f);
            Vector3f normal = Vector3f.UNIT_Z;
            BlockHitResult hit = new BlockHitResult(new Vector3f(5.5f, 10.1f, 15.9f), blockPos, normal, 2.5f);

            assertThat(hit.getBlockX()).isEqualTo(5);
            assertThat(hit.getBlockY()).isEqualTo(10);
            assertThat(hit.getBlockZ()).isEqualTo(15);
            assertThat(hit.getPlacementX()).isEqualTo(5);
            assertThat(hit.getPlacementY()).isEqualTo(10);
            assertThat(hit.getPlacementZ()).isEqualTo(16);
            assertThat(hit.getPlacementPosition()).isEqualTo(new Vector3f(5f, 10f, 16f));
        }

        @Test
        void normalizeToCardinalChoosesDominantAxis() {
            Vector3f alongX = BlockPicker.normalizeToCardinal(new Vector3f(0.9f, 0.1f, 0.2f));
            Vector3f alongNegativeY = BlockPicker.normalizeToCardinal(new Vector3f(-0.2f, -0.8f, 0.1f));
            Vector3f alongNegativeZ = BlockPicker.normalizeToCardinal(new Vector3f(0.3f, 0.4f, -0.9f));

            assertThat(alongX).isEqualTo(Vector3f.UNIT_X);
            assertThat(alongNegativeY).isEqualTo(Vector3f.UNIT_Y.negate());
            assertThat(alongNegativeZ).isEqualTo(Vector3f.UNIT_Z.negate());
        }

        @Test
        void floorVectorFloorsAllComponents() {
            Vector3f floored = BlockPicker.floorVector(new Vector3f(3.7f, -1.2f, 0.0f));
            Vector3f zero = BlockPicker.floorVector(null);

            assertThat(floored).isEqualTo(new Vector3f(3f, -2f, 0f));
            assertThat(zero).isEqualTo(Vector3f.ZERO);
        }

        @Test
        void faceNormalOffsetKeepsBlockCoordinateStable() {
            Vector3f contact = new Vector3f(10f, 5f, 15f);
            Vector3f faceNormal = Vector3f.UNIT_Y;
            Vector3f blockPosition = BlockPicker.floorVector(contact.subtract(faceNormal.mult(0.001f)));

            assertThat(blockPosition).isEqualTo(new Vector3f(10f, 4f, 15f));
        }
    }

    @Nested
    class IntegrationLikeTests {

        @Test
        void placeAndBreakRoundTripKeepsInventoryAndWorldInSync() {
            Inventory inventory = new Inventory();
            inventory.setSlot(0, ItemStack.of(BlockType.STONE, 2));

            AtomicInteger notifications = new AtomicInteger();
            inventory.addListener(inv -> notifications.incrementAndGet());
            notifications.set(0);

            ChunkStorage storage = new ChunkStorage();
            Chunk chunk = new Chunk(new ChunkPos(0, 0));
            storage.putChunk(chunk);

            int x = 1;
            int y = 64;
            int z = 1;
            assertThat(storage.getBlock(x, y, z)).isEqualTo(BlockType.AIR.getId());

            boolean removed = inventory.removeFromSlot(0, 1);
            assertThat(removed).isTrue();
            storage.setBlock(x, y, z, BlockType.STONE.getId());

            ItemStack afterPlacement = inventory.getSlot(0);
            assertThat(afterPlacement).isNotNull();
            assertThat(afterPlacement.count()).isEqualTo(1);
            assertThat(storage.getBlock(x, y, z)).isEqualTo(BlockType.STONE.getId());

            boolean added = inventory.addItem(BlockType.STONE, 1);
            assertThat(added).isTrue();
            storage.setBlock(x, y, z, BlockType.AIR.getId());

            ItemStack afterBreak = inventory.getSlot(0);
            assertThat(afterBreak).isNotNull();
            assertThat(afterBreak.count()).isEqualTo(2);
            assertThat(storage.getBlock(x, y, z)).isEqualTo(BlockType.AIR.getId());
            assertThat(notifications.get()).isEqualTo(2);
        }
    }
}
