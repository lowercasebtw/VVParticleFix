package us.myles.ViaVersion.protocols.protocol1_14to1_13_2.packets;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData;

public class InventoryPackets {

    public static void register(Protocol protocol) {

        /*
            Outgoing packets
         */

        // Set slot packet
        protocol.registerOutgoing(State.PLAY, 0x17, 0x17, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot ID
                map(Type.FLAT_VAR_INT_ITEM); // 2 - Slot Value

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        toClient(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

        // Window items packet
        protocol.registerOutgoing(State.PLAY, 0x15, 0x15, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.FLAT_VAR_INT_ITEM_ARRAY); // 1 - Window Values

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item[] stacks = wrapper.get(Type.FLAT_VAR_INT_ITEM_ARRAY, 0);
                        for (Item stack : stacks) toClient(stack);
                    }
                });
            }
        });

        // Plugin message
        protocol.registerOutgoing(State.PLAY, 0x19, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Channel
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        String channel = wrapper.get(Type.STRING, 0);
                        if (channel.equals("minecraft:trader_list") || channel.equals("trader_list")) {
                            wrapper.passthrough(Type.INT); // Passthrough Window ID

                            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                            for (int i = 0; i < size; i++) {
                                // Input Item
                                toClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                                // Output Item
                                toClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));

                                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                                if (secondItem) {
                                    // Second Item
                                    toClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                                }

                                wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                                wrapper.passthrough(Type.INT); // Number of tools uses
                                wrapper.passthrough(Type.INT); // Maximum number of trade uses
                            }
                        }
                    }
                });
            }
        });

        // Entity Equipment Packet
        protocol.registerOutgoing(State.PLAY, 0x42, 0x42, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.FLAT_VAR_INT_ITEM); // 2 - Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        toClient(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

        // Declare Recipes
        protocol.registerOutgoing(State.PLAY, 0x54, 0x55, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int size = wrapper.passthrough(Type.VAR_INT);
                        int deleted = 0;
                        for (int i = 0; i < size; i++) {
                            String id = wrapper.read(Type.STRING); // Recipe Identifier
                            String type = wrapper.read(Type.STRING);
                            if (type.equals("crafting_special_banneraddpattern")) {
                                deleted++;
                                continue;
                            }
                            wrapper.write(Type.STRING, id);
                            wrapper.write(Type.STRING, type);

                            if (type.equals("crafting_shapeless")) {
                                wrapper.passthrough(Type.STRING); // Group
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT);
                                for (int j = 0; j < ingredientsNo; j++) {
                                    Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    for (Item item : items) toClient(item);
                                }
                                toClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
                            } else if (type.equals("crafting_shaped")) {
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                                wrapper.passthrough(Type.STRING); // Group
                                for (int j = 0; j < ingredientsNo; j++) {
                                    Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    for (Item item : items) toClient(item);
                                }
                                toClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
                            } else if (type.equals("smelting")) {
                                wrapper.passthrough(Type.STRING); // Group
                                Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                for (Item item : items) toClient(item);
                                toClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                                wrapper.passthrough(Type.FLOAT); // EXP
                                wrapper.passthrough(Type.VAR_INT); // Cooking time
                            }
                        }
                        wrapper.set(Type.VAR_INT, 0, size - deleted);
                    }
                });
            }
        });


        /*
            Incoming packets
         */

        // Click window packet
        protocol.registerIncoming(State.PLAY, 0x08, 0x08, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot
                map(Type.BYTE); // 2 - Button
                map(Type.SHORT); // 3 - Action number
                map(Type.VAR_INT); // 4 - Mode
                map(Type.FLAT_VAR_INT_ITEM); // 5 - Clicked Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        toServer(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

        // Creative Inventory Action
        protocol.registerIncoming(State.PLAY, 0x24, 0x24, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.SHORT); // 0 - Slot
                map(Type.FLAT_VAR_INT_ITEM); // 1 - Clicked Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        toServer(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });
    }


    public static void toClient(Item item) {
        if (item == null) return;
        item.setId((short) getNewItemId(item.getId()));
    }

    public static int getNewItemId(int id) {
        Integer newId = MappingData.oldToNewItems.get(id);
        if (newId == null) {
            Via.getPlatform().getLogger().warning("Missing 1.14 item for 1.13.2 item " + id);
            return 1;
        }
        return newId;
    }

    public static void toServer(Item item) {
        if (item == null) return;
        item.setId((short) getOldItemId(item.getId()));
    }

    public static int getOldItemId(int id) {
        Integer oldId = MappingData.oldToNewItems.inverse().get(id);
        return oldId != null ? oldId : 1;
    }
}