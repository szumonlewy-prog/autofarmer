package com.example.autofarmer;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class FarmerBlockEntity extends BlockEntity {
    private static final int MAX_SIZE = 32;    // max wymiar X i Z obszaru
    private static final int MAX_HEIGHT = 4;   // max liczba warstw (Y)
    private static final int TICKS_PER_ACTION = 5;
    private static final int SCAN_PER_ACTION = 24;

    private BlockPos source;
    private BlockPos output;
    private BlockPos min;
    private BlockPos max;
    private Item seed = Items.WHEAT_SEEDS;
    private boolean enabled = false;

    private int cooldown = 0;
    private int cursor = 0;

    public FarmerBlockEntity(BlockPos pos, BlockState state) {
        super(AutoFarmerMod.FARMER_BE, pos, state);
        this.source = pos;
        this.output = pos;
        this.min = pos;
        this.max = pos;
    }

    // ---------- konfiguracja ----------

    public ConfigPayload toPayload() {
        return new ConfigPayload(pos, source, output, min, max,
                Registries.ITEM.getId(seed).toString(), enabled);
    }

    public void applyConfig(ConfigPayload p) {
        this.source = p.source();
        this.output = p.output();

        BlockPos a = p.min(), b = p.max();
        BlockPos mn = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos mx = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        mx = new BlockPos(
                Math.min(mx.getX(), mn.getX() + MAX_SIZE - 1),
                Math.min(mx.getY(), mn.getY() + MAX_HEIGHT - 1),
                Math.min(mx.getZ(), mn.getZ() + MAX_SIZE - 1));
        this.min = mn;
        this.max = mx;

        Identifier id = Identifier.tryParse(p.seed());
        if (id != null) {
            Item item = Registries.ITEM.get(id);
            if (FarmerSeeds.isAllowed(item)) this.seed = item;
        }
        this.enabled = p.enabled();
        this.cursor = 0;
        markDirty();
    }

    // ---------- tick ----------

    public static void tick(World world, BlockPos pos, BlockState state, FarmerBlockEntity be) {
        if (!(world instanceof ServerWorld sw) || !be.enabled) return;
        if (++be.cooldown < TICKS_PER_ACTION) return;
        be.cooldown = 0;
        be.work(sw);
    }

    private void work(ServerWorld sw) {
        int sx = max.getX() - min.getX() + 1;
        int sy = max.getY() - min.getY() + 1;
        int sz = max.getZ() - min.getZ() + 1;
        int total = sx * sy * sz;
        if (total <= 0) return;

        for (int i = 0; i < Math.min(total, SCAN_PER_ACTION); i++) {
            cursor = (cursor + 1) % total;
            int ix = cursor % sx;
            int iz = (cursor / sx) % sz;
            int iy = cursor / (sx * sz);
            BlockPos p = min.add(ix, iy, iz);

            if (!sw.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) continue;

            if (tryHarvest(sw, p) || tryPlant(sw, p, ix, iz)) return;
        }
    }

    private Storage<ItemVariant> storage(ServerWorld sw, BlockPos p) {
        if (!sw.getChunkManager().isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) return null;
        return ItemStorage.SIDED.find(sw, p, Direction.UP);
    }

    // ---------- zbiór ----------

    private boolean tryHarvest(ServerWorld sw, BlockPos p) {
        BlockState st = sw.getBlockState(p);
        Block b = st.getBlock();

        boolean crop = false;
        Item replantSeed = null;

        if (b instanceof CropBlock cb) {
            if (!cb.isMature(st)) return false;
            crop = true;
            replantSeed = FarmerSeeds.seedForCrop(b);
        } else if (b == Blocks.MELON || b == Blocks.PUMPKIN) {
            if (FarmerSeeds.fruitFor(seed) != b) return false;
        } else {
            return false;
        }

        Storage<ItemVariant> out = storage(sw, output);
        if (out == null) return false;

        List<ItemStack> drops = new ArrayList<>(Block.getDroppedStacks(st, sw, p, null));

        // ponowne zasadzenie kosztuje jedno nasiono / jedną sztukę
        if (replantSeed != null) {
            for (ItemStack s : drops) {
                if (s.isOf(replantSeed)) {
                    s.decrement(1);
                    break;
                }
            }
        }

        if (!insertAll(out, drops)) return false; // skrzynka wyjściowa pełna -> nie zbieramy

        if (crop) {
            sw.setBlockState(p, ((CropBlock) b).withAge(0));
        } else {
            sw.setBlockState(p, Blocks.AIR.getDefaultState());
        }
        return true;
    }

    private boolean insertAll(Storage<ItemVariant> out, List<ItemStack> stacks) {
        try (Transaction t = Transaction.openOuter()) {
            for (ItemStack s : stacks) {
                if (s.isEmpty()) continue;
                long inserted = out.insert(ItemVariant.of(s), s.getCount(), t);
                if (inserted < s.getCount()) return false; // brak miejsca -> rollback
            }
            t.commit();
            return true;
        }
    }

    // ---------- sadzenie ----------

    private boolean tryPlant(ServerWorld sw, BlockPos p, int ix, int iz) {
        if (!sw.getBlockState(p).isAir()) return false;

        Block crop = FarmerSeeds.cropBlock(seed);
        if (crop == null) return false;

        // Dynie/arbuzy: sadzimy co drugi kafelek, żeby owoc miał gdzie wyrosnąć
        if (FarmerSeeds.isStem(seed) && (ix % 2 != 0 || iz % 2 != 0)) return false;

        BlockState ns = crop.getDefaultState();
        if (!ns.canPlaceAt(sw, p)) return false; // wymaga farmlandu (i światła)

        Storage<ItemVariant> src = storage(sw, source);
        if (src == null) return false;

        try (Transaction t = Transaction.openOuter()) {
            if (src.extract(ItemVariant.of(seed), 1, t) == 1) {
                t.commit();
                sw.setBlockState(p, ns);
                return true;
            }
        }
        return false;
    }

    // ---------- NBT ----------

    private static NbtCompound dummy() { return new NbtCompound(); }

    private static void putPos(NbtCompound nbt, String key, BlockPos p) {
        nbt.putIntArray(key, new int[]{p.getX(), p.getY(), p.getZ()});
    }

    private static BlockPos getPos(NbtCompound nbt, String key, BlockPos def) {
        int[] a = nbt.getIntArray(key);
        return a.length == 3 ? new BlockPos(a[0], a[1], a[2]) : def;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        putPos(nbt, "Source", source);
        putPos(nbt, "Output", output);
        putPos(nbt, "Min", min);
        putPos(nbt, "Max", max);
        nbt.putString("Seed", Registries.ITEM.getId(seed).toString());
        nbt.putBoolean("Enabled", enabled);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        source = getPos(nbt, "Source", pos);
        output = getPos(nbt, "Output", pos);
        min = getPos(nbt, "Min", pos);
        max = getPos(nbt, "Max", pos);
        Identifier id = Identifier.tryParse(nbt.getString("Seed"));
        if (id != null) {
            Item item = Registries.ITEM.get(id);
            if (FarmerSeeds.isAllowed(item)) seed = item;
        }
        enabled = nbt.getBoolean("Enabled");
    }
}
