package dev.covenant.mixin;

import dev.covenant.rpg.progression.ProfessionHarvesting;
import net.minecraft.advancements.criterion.BredAnimalsTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native successful breeding chooses one breeder, including vanilla species with special breeding paths. */
@Mixin(BredAnimalsTrigger.class)
abstract class ProfessionBreedingMixin {
    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/animal/Animal;Lnet/minecraft/world/entity/animal/Animal;Lnet/minecraft/world/entity/AgeableMob;)V", at = @At("TAIL"))
    private void covenant$bred(ServerPlayer player, Animal parent, Animal partner, AgeableMob child, CallbackInfo ci) {
        ProfessionHarvesting.bred(player, (net.minecraft.server.level.ServerLevel) parent.level());
    }
}
