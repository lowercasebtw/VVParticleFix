package us.myles.ViaVersion.api.type.types.version;

import io.netty.buffer.ByteBuf;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.util.CompactArrayUtil;

public class ChunkSectionType1_16 extends Type<ChunkSection> {
    private static final int GLOBAL_PALETTE = 14;

    public ChunkSectionType1_16() {
        super("Chunk Section Type", ChunkSection.class);
    }

    @Override
    public ChunkSection read(ByteBuf buffer) throws Exception {
        ChunkSection chunkSection = new ChunkSection();

        // Reaad bits per block
        int bitsPerBlock = buffer.readUnsignedByte();
        int originalBitsPerBlock = bitsPerBlock;

        if (bitsPerBlock == 0 || bitsPerBlock > 8) {
            bitsPerBlock = GLOBAL_PALETTE;
        }

        int paletteLength = bitsPerBlock == GLOBAL_PALETTE ? 0 : Type.VAR_INT.read(buffer);
        // Read palette
        chunkSection.clearPalette();
        for (int i = 0; i < paletteLength; i++) {
            chunkSection.addPaletteEntry(Type.VAR_INT.read(buffer));
        }

        // Read blocks
        long[] blockData = new long[Type.VAR_INT.read(buffer)];
        if (blockData.length > 0) {
            char valuesPerLong = (char) (64 / bitsPerBlock);
            int expectedLength = (ChunkSection.SIZE + valuesPerLong - 1) / valuesPerLong;
            if (blockData.length != expectedLength) {
                throw new IllegalStateException("Block data length (" + blockData.length + ") does not match expected length (" + expectedLength + ")! bitsPerBlock=" + bitsPerBlock + ", originalBitsPerBlock=" + originalBitsPerBlock);
            }

            for (int i = 0; i < blockData.length; i++) {
                blockData[i] = buffer.readLong();
            }
            CompactArrayUtil.iterateCompactArrayWithPadding(bitsPerBlock, ChunkSection.SIZE, blockData,
                    bitsPerBlock == GLOBAL_PALETTE ? chunkSection::setFlatBlock : chunkSection::setPaletteIndex);
        }

        return chunkSection;
    }

    @Override
    public void write(ByteBuf buffer, ChunkSection chunkSection) throws Exception {
        int bitsPerBlock = 4;
        while (chunkSection.getPaletteSize() > 1 << bitsPerBlock) {
            bitsPerBlock += 1;
        }

        if (bitsPerBlock > 8) {
            bitsPerBlock = GLOBAL_PALETTE;
        }

        buffer.writeByte(bitsPerBlock);

        // Write pallet (or not)
        if (bitsPerBlock != GLOBAL_PALETTE) {
            Type.VAR_INT.write(buffer, chunkSection.getPaletteSize());
            for (int i = 0; i < chunkSection.getPaletteSize(); i++) {
                Type.VAR_INT.write(buffer, chunkSection.getPaletteEntry(i));
            }
        }

        long[] data = CompactArrayUtil.createCompactArrayWithPadding(bitsPerBlock, ChunkSection.SIZE,
                bitsPerBlock == GLOBAL_PALETTE ? chunkSection::getFlatBlock : chunkSection::getPaletteIndex);
        Type.VAR_INT.write(buffer, data.length);
        for (long l : data) {
            buffer.writeLong(l);
        }
    }
}