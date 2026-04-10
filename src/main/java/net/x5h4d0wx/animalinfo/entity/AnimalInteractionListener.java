package net.x5h4d0wx.animalinfo.entity;


import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnimalInteractionListener implements UseEntityCallback {
    public static final double HORSE_SPEED_CONVERSION = 42.133333333333333;
    public static final double JUMP_A = 4.125;
    public static final double JUMP_B = 1.125;
    public static AtomicBoolean exec_scheduled = new AtomicBoolean(false);
    public static AtomicBoolean exec_ended = new AtomicBoolean(true);
    private static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);

    final private static Map<Integer, Long> cooldownEntity = new HashMap<>();
    
    public AnimalInteractionListener(){
        executor.setMaximumPoolSize(2);
    }
    @Override
    public InteractionResult interact(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        Component outText = null;
        // CommandDispatcher<FabricClientCommandSource> test = ClientCommandManager.getActiveDispatcher();
        // player.sendMessage(Text.of(String.valueOf(test==null)), true);
        exec_scheduled.set(false);

        if (!player.isPassenger() && !player.isCrouching()){
            return InteractionResult.PASS;
        }

        int id = entity.getId();
        if (cooldownEntity.containsKey(id) && System.currentTimeMillis() < cooldownEntity.get(id)){
            return InteractionResult.PASS;
        }
        cooldownEntity.put(id, System.currentTimeMillis() + 200);

        switch (entity) {
            case Panda panda:
                // From the original Panda Info
                MutableComponent mainGene = Component.translatable("gui.panda-info.gene." + panda.getMainGene().getSerializedName().toUpperCase());
                MutableComponent hiddenGene = Component.translatable("gui.panda-info.gene." + panda.getHiddenGene().getSerializedName().toUpperCase());
                MutableComponent productGene = Component.translatable("gui.panda-info.gene." + panda.getVariant().getSerializedName().toUpperCase());

                mainGene = Component.translatable("gui.panda-info.main_gene", "§6", mainGene);
                hiddenGene = Component.translatable("gui.panda-info.hidden_gene", "§b", hiddenGene);
                productGene = Component.translatable("gui.panda-info.product_gene", "§a", productGene);

                outText = ComponentUtils.formatList(List.of(mainGene, hiddenGene, productGene), Component.literal(" §r| "));

                break;

            case AbstractHorse horsish:
                final double blockSpeed = horsish.getAttributeValue(Attributes.MOVEMENT_SPEED)* HORSE_SPEED_CONVERSION;
                final double blockJump = jumpStrToBlocks(horsish.getAttributeValue(Attributes.JUMP_STRENGTH));
                final double health = horsish.getAttributeValue(Attributes.MAX_HEALTH);
                outText = Component.literal(String.format("§6Speed: %1$.2f §r| §bJump %2$.2f §r| §aHearts %3$.2f ",blockSpeed, blockJump, health/2));

                break;

            case Goat goat:
                if (goat.isScreamingGoat()){
                    outText = Component.literal("§bScreaming Goat");
                }
                else {
                    outText = Component.literal("Normal Goat");
                }

                break;

            default:
                break;
        }

        if (outText != null) {
            sendLingeringMessage(player, entity, outText);
        }

        return InteractionResult.PASS;
    }

    private static double jumpStrToBlocks(double jumpPower){
        return (JUMP_A * jumpPower * jumpPower) + (JUMP_B * jumpPower);
    }

    private static void lingeringMessageHandeler(Player player, Entity entity ,Component message){
        player.sendOverlayMessage(message);
        if(player.isWithinEntityInteractionRange(entity,0) && exec_scheduled.get()){
            executor.schedule(() -> lingeringMessageHandeler(player, entity, message), 100, TimeUnit.MILLISECONDS);
        }else{
            exec_ended.set(true);
        }
    }

    private static void sendLingeringMessage(Player player, Entity entity ,Component message){
        final String messageCopy = message.getString();
        while (!exec_ended.get()) {}
        exec_scheduled.set(true);
        exec_ended.set(false);
        lingeringMessageHandeler(player, entity, Component.literal(messageCopy));
    }
}