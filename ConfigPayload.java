package com.example.autofarmer;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Jeden pakiet w dwie strony:
 *  S2C = "otwórz GUI z tą konfiguracją",
 *  C2S = "zapisz tę konfigurację".
 */
public record ConfigPayload(BlockPos pos, BlockPos source, BlockPos output,
                            BlockPos min, BlockPos max, String seed, boolean enabled)
        implements CustomPayload {

    public static final CustomPayload.Id<ConfigPayload> ID =
            new CustomPayload.Id<>(Identifier.of(AutoFarmerMod.MOD_ID, "config"));

    public static final PacketCodec<PacketByteBuf, ConfigPayload> CODEC =
            CustomPayload.codecOf(ConfigPayload::write, ConfigPayload::new);

    public ConfigPayload(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readBlockPos(), buf.readBlockPos(),
                buf.readBlockPos(), buf.readBlockPos(), buf.readString(), buf.readBoolean());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBlockPos(source);
        buf.writeBlockPos(output);
        buf.writeBlockPos(min);
        buf.writeBlockPos(max);
        buf.writeString(seed);
        buf.writeBoolean(enabled);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
