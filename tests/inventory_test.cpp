#include "poorcraft/core/Inventory.h"

#include <gtest/gtest.h>

namespace poorcraft::core {

TEST(InventoryTest, InitialState)
{
    Inventory inventory;
    EXPECT_EQ(inventory.getSelectedSlot(), 0);
    EXPECT_EQ(inventory.getSlot(0), world::BlockType::Grass);
    EXPECT_EQ(inventory.getSlot(1), world::BlockType::Dirt);
    EXPECT_EQ(inventory.getSlot(2), world::BlockType::Stone);
    for(int i = 3; i < HOTBAR_SIZE; ++i)
    {
        EXPECT_EQ(inventory.getSlot(i), world::BlockType::Air);
    }
}

TEST(InventoryTest, SetSelectedSlotClamps)
{
    Inventory inventory;
    inventory.setSelectedSlot(4);
    EXPECT_EQ(inventory.getSelectedSlot(), 4);

    inventory.setSelectedSlot(-1);
    EXPECT_EQ(inventory.getSelectedSlot(), 0);

    inventory.setSelectedSlot(100);
    EXPECT_EQ(inventory.getSelectedSlot(), HOTBAR_SIZE - 1);
}

TEST(InventoryTest, NextPreviousSlotWraps)
{
    Inventory inventory;
    inventory.setSelectedSlot(HOTBAR_SIZE - 1);
    inventory.nextSlot();
    EXPECT_EQ(inventory.getSelectedSlot(), 0);

    inventory.previousSlot();
    EXPECT_EQ(inventory.getSelectedSlot(), HOTBAR_SIZE - 1);
}

TEST(InventoryTest, GetSetSlot)
{
    Inventory inventory;
    inventory.setSlot(4, world::BlockType::Grass);
    EXPECT_EQ(inventory.getSlot(4), world::BlockType::Grass);

    inventory.setSlot(-1, world::BlockType::Stone);
    EXPECT_EQ(inventory.getSlot(-1), world::BlockType::Air);
}

TEST(InventoryTest, GetSelectedBlock)
{
    Inventory inventory;
    EXPECT_EQ(inventory.getSelectedBlock(), world::BlockType::Grass);

    inventory.setSelectedSlot(2);
    EXPECT_EQ(inventory.getSelectedBlock(), world::BlockType::Stone);
}

} // namespace poorcraft::core
