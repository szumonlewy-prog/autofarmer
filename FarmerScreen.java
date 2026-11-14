package com.example.autofarmer.client;

import com.example.autofarmer.ConfigPayload;
import com.example.autofarmer.FarmerSeeds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class FarmerScreen extends Screen {
    private static final String[] LABELS = {
            "Skrzynka Z NASIONAMI (X Y Z)",
            "Skrzynka NA PLONY (X Y Z)",
            "Obszar sadzenia - róg 1 (X Y Z)",
            "Obszar sadzenia - róg 2 (X Y Z)"
    };

    private final ConfigPayload data;
    private final TextFieldWidget[][] fields = new TextFieldWidget[4][3];
    private int seedIndex;
    private boolean enabled;
    private ButtonWidget seedButton;
    private ButtonWidget toggleButton;

    public FarmerScreen(ConfigPayload data) {
        super(Text.literal("Auto Farmer"));
        this.data = data;
        this.enabled = data.enabled();

        Identifier id = Identifier.tryParse(data.seed());
        Item cur = id == null ? null : Registries.ITEM.get(id);
        int idx = cur == null ? 0 : FarmerSeeds.SEEDS.indexOf(cur);
        this.seedIndex = Math.max(idx, 0);
    }

    @Override
    protected void init() {
        BlockPos[] initial = {data.source(), data.output(), data.min(), data.max()};
        int left = this.width / 2 - 125;
        int y0 = 26;

        for (int r = 0; r < 4; r++) {
            int y = y0 + r * 38 + 11;
            int[] vals = {initial[r].getX(), initial[r].getY(), initial[r].getZ()};
            for (int c = 0; c < 3; c++) {
                TextFieldWidget tf = new TextFieldWidget(this.textRenderer, left + c * 54, y, 50, 18, Text.literal("xyz"));
                tf.setMaxLength(9);
                tf.setTextPredicate(s -> s.matches("-?\\d*"));
                tf.setText(String.valueOf(vals[c]));
                fields[r][c] = tf;
                addDrawableChild(tf);
            }
            final int row = r;
            addDrawableChild(ButtonWidget.builder(Text.literal("Moja poz."), b -> {
                BlockPos p = this.client.player.getBlockPos();
                fields[row][0].setText(String.valueOf(p.getX()));
                fields[row][1].setText(String.valueOf(p.getY()));
                fields[row][2].setText(String.valueOf(p.getZ()));
            }).dimensions(left + 168, y - 1, 82, 20).build());
        }

        int by = y0 + 4 * 38 + 4;

        seedButton = addDrawableChild(ButtonWidget.builder(seedText(), b -> {
            seedIndex = (seedIndex + 1) % FarmerSeeds.SEEDS.size();
            b.setMessage(seedText());
        }).dimensions(this.width / 2 - 125, by, 122, 20).build());

        toggleButton = addDrawableChild(ButtonWidget.builder(toggleText(), b -> {
            enabled = !enabled;
            b.setMessage(toggleText());
        }).dimensions(this.width / 2 + 3, by, 122, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz i zamknij"), b -> {
            save();
            close();
        }).dimensions(this.width / 2 - 100, by + 24, 200, 20).build());
    }

    private Text seedText() {
        return Text.literal("Sadź: ").append(FarmerSeeds.SEEDS.get(seedIndex).getName());
    }

    private Text toggleText() {
        return Text.literal(enabled ? "Praca: WŁĄCZONA" : "Praca: WYŁĄCZONA");
    }

    private BlockPos readRow(int r, BlockPos fallback) {
        int[] v = {fallback.getX(), fallback.getY(), fallback.getZ()};
        for (int c = 0; c < 3; c++) {
            try {
                v[c] = Integer.parseInt(fields[r][c].getText().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return new BlockPos(v[0], v[1], v[2]);
    }

    private void save() {
        Item seed = FarmerSeeds.SEEDS.get(seedIndex);
        ClientPlayNetworking.send(new ConfigPayload(
                data.pos(),
                readRow(0, data.source()),
                readRow(1, data.output()),
                readRow(2, data.min()),
                readRow(3, data.max()),
                Registries.ITEM.getId(seed).toString(),
                enabled));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Y obszaru = warstwa roślin (blok nad farmlandem)"), this.width / 2, 18, 0xAAAAAA);
        int left = this.width / 2 - 125;
        for (int r = 0; r < 4; r++) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(LABELS[r]), left, 26 + r * 38, 0xFFFFFF);
        }
    }
}
