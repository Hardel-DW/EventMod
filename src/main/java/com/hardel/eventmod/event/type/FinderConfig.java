package com.hardel.eventmod.event.type;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FinderConfig(
        UUID uuid,
        String variant,
        Text foundMessage,
        Text alreadyFoundMessage,
        String reward,
        Identifier sound,
        SimpleParticleType particle
) {
    private static final Text defaultFoundMessage = Text.of("You found a new one!");
    private static final Text defaultAlreadyFoundMessage = Text.of("You already found this one!");
    private static final Identifier defaultSound = Identifier.of("entity.experience_orb.pickup");
    private static final SimpleParticleType defaultParticle = ParticleTypes.HAPPY_VILLAGER;
    private static final String defaultReward = "nyanmod:nyantite";

    private FinderConfig(Builder builder) {
        this(builder.uuid, builder.variant, builder.foundMessage, builder.alreadyFoundMessage, builder.reward, builder.sound, builder.particle);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID uuid;
        private String variant;
        private Text foundMessage = defaultFoundMessage;
        private Text alreadyFoundMessage = defaultAlreadyFoundMessage;
        private String reward = defaultReward;
        private Identifier sound = defaultSound;
        private SimpleParticleType particle = defaultParticle;

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder variant(String variant) {
            this.variant = variant;
            return this;
        }

        public Builder foundMessage(Text foundMessage) {
            this.foundMessage = foundMessage;
            return this;
        }

        public Builder alreadyFoundMessage(Text alreadyFoundMessage) {
            this.alreadyFoundMessage = alreadyFoundMessage;
            return this;
        }

        public Builder reward(String reward) {
            this.reward = reward;
            return this;
        }

        public Builder sound(Identifier sound) {
            this.sound = sound;
            return this;
        }

        public Builder particle(SimpleParticleType particle) {
            this.particle = particle;
            return this;
        }

        public FinderConfig build() {
            return new FinderConfig(this);
        }
    }
}