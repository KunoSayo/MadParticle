package cn.usshenzhou.madparticle.particle;

import cn.usshenzhou.madparticle.Madparticle;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author USS_Shenzhou
 * <p>
 * File name: modid~leaves.png  modid~leaves#1.png
 * <p>
 * Particle type reg name: modid:leaves
 * <p>
 * Particle texture name in json: modid:leaves modid:leaves__1
 */
public class CustomParticleRegistry {
    public static final LinkedHashMap<ResourceLocation, List<ResourceLocation>> CUSTOM_PARTICLES_TYPE_NAMES_AND_TEXTURES = new LinkedHashMap<>();
    public static final LinkedHashSet<ParticleType<SimpleParticleType>> CUSTOM_PARTICLE_TYPES = new LinkedHashSet<>();
    public static final LinkedHashSet<ResourceLocation> ALL_TEXTURES = new LinkedHashSet<>();

    public static void registerParticleType() {
        File particleDir = new File(Minecraft.getInstance().gameDirectory, "customparticles");
        if (!particleDir.exists() || !particleDir.isDirectory()) {
            particleDir.mkdir();
            return;
        }
        File[] files = particleDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) {
            return;
        }
        Arrays.stream(files).filter(file -> !file.isDirectory()).forEach(file -> {
            ResourceLocation particleTypeName = fileToParticleTypeNameResLoc(file);
            if (particleTypeName == null) {
                return;
            }
            if (CUSTOM_PARTICLES_TYPE_NAMES_AND_TEXTURES.containsKey(particleTypeName)) {
                return;
            }
            List<ResourceLocation> textureNames = fileToTextureName(files, file);
            CUSTOM_PARTICLES_TYPE_NAMES_AND_TEXTURES.put(particleTypeName, textureNames);
            ALL_TEXTURES.addAll(textureNames);
            ParticleType<SimpleParticleType> particleType = new SimpleParticleType(false);
            CUSTOM_PARTICLE_TYPES.add(particleType);
            Registry.register(Registry.PARTICLE_TYPE, particleTypeName, particleType);
        });
    }

    public static void onParticleProviderRegistry() {
        CUSTOM_PARTICLE_TYPES.forEach(particleType -> ParticleFactoryRegistry.getInstance().register(particleType, GeneralNullProvider::new));
    }

    public static final String MOD_ID_SPLIT = "~";
    public static final String AGE_SPLIT = "#";

    public static @Nullable ResourceLocation fileToParticleTypeNameResLoc(File file) {
        String name = file.getName().toLowerCase().replace(".png", "").split(AGE_SPLIT)[0];
        if (!name.contains(MOD_ID_SPLIT)) {
            name = Madparticle.MOD_ID + "~" + name;
        }
        name = name.replace(MOD_ID_SPLIT, ":");
        try {
            return new ResourceLocation(name);
        } catch (ResourceLocationException ignored) {
            LogUtils.getLogger().error("Failed to register particle {}. This is not a valid resource location.", file.getName());
            return null;
        }
    }

    public static List<ResourceLocation> fileToTextureName(File[] allFile, File file) {
        String key = file.getName().replace(".png", "").split(AGE_SPLIT)[0];
        List<ResourceLocation> locations = new ArrayList<>();
        //This will cause an n*n in the worst situation. But it's in the loading stage. Just wait.
        List<File> textures = Arrays.stream(allFile).filter(f -> f.getName().contains(key)).toList();
        if (textures.size() > 1) {
            for (int i = 0; i < textures.size(); i++) {
                String supposedName = key + "#" + i + ".png";
                Optional<File> optional = textures.stream().filter(f -> f.getName().contains(supposedName)).findFirst();
                if (optional.isEmpty()) {
                    LogUtils.getLogger().error("Failed to find texture {}. Check if it is a valid name.", supposedName);
                    continue;
                }
                ResourceLocation location = fileToTextureResLoc(optional.get());
                if (location != null) {
                    locations.add(location);
                }
            }
        } else {
            ResourceLocation location = fileToTextureResLoc(textures.get(0));
            if (location != null) {
                locations.add(location);
            }
        }
        return locations;
    }

    private static @Nullable ResourceLocation fileToTextureResLoc(File file) {
        String s = file.getName().replace(".png", "").replace(MOD_ID_SPLIT, ":").replace(AGE_SPLIT, "__");
        if (!s.contains(":")) {
            s = Madparticle.MOD_ID + ":" + s;
        }
        try {
            return new ResourceLocation(s);
        } catch (ResourceLocationException ignored) {
            LogUtils.getLogger().error("Failed to register texture {}. This is not a valid resource location.", file.getName());
            return null;
        }
    }

    public static String listToJsonString(List<ResourceLocation> locations) {
        StringBuilder builder = new StringBuilder();
        locations.forEach(location -> builder
                .append("\"")
                .append(location.toString())
                .append("\",\n")
        );
        builder.delete(builder.length() - 2, builder.length());
        return builder.toString();
    }

    public static File textureResLocToFile(ResourceLocation location) throws FileNotFoundException {
        String name = location.toString().replace(Madparticle.MOD_ID + ":", "").replace(":", MOD_ID_SPLIT).replace("__", "#") + ".png";
        File file = new File(Minecraft.getInstance().gameDirectory, "customparticles/" + name);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        return file;
    }

    public static class GeneralNullProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public GeneralNullProvider(SpriteSet pSprites) {
            this.sprite = pSprites;
        }

        public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            return new CustomParticle(pLevel, pX, pY, pZ);
        }
    }
}
