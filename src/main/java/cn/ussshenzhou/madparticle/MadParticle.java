package cn.ussshenzhou.madparticle;

import cn.ussshenzhou.madparticle.command.inheritable.ModCommandArgumentRegistry;
import cn.ussshenzhou.madparticle.item.ModItemsRegistry;
import cn.ussshenzhou.madparticle.particle.ModParticleTypeRegistry;
import cn.ussshenzhou.t88.config.ConfigHelper;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author USS_Shenzhou
 */
@Mod("madparticle")
public class MadParticle {
    public static final String MOD_ID = "madparticle";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean IS_OPTIFINE_INSTALLED = isClassFound("net.optifine.reflect.ReflectorClass");
    public static final boolean IS_SHIMMER_INSTALLED = ModList.get().isLoaded("shimmer");
    public static boolean irisOn;

    public MadParticle(IEventBus modEventBus) {
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);
        NeoForge.EVENT_BUS.register(this);
        ModParticleTypeRegistry.PARTICLE_TYPES.register(modEventBus);
        ModCommandArgumentRegistry.COMMAND_ARGUMENTS.register(modEventBus);
        ModItemsRegistry.ITEMS.register(modEventBus);
    }

    public boolean isModLoaded(String modID) {
        return ModList.get().isLoaded(modID);
    }

    public static boolean isClassFound(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void runOnShimmer(Supplier<Runnable> run) {
        if (IS_SHIMMER_INSTALLED) {
            run.get().run();
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("WelCome to the world of MadParticle!");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        ConfigHelper.loadConfig(new MadParticleConfig());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            try {
                Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstance = irisApi.getMethod("getInstance");
                getInstance.setAccessible(true);
                Method isShaderPackInUse = irisApi.getMethod("isShaderPackInUse");
                isShaderPackInUse.setAccessible(true);
                irisOn = (boolean) isShaderPackInUse.invoke(getInstance.invoke(null));
            } catch (Exception ignored) {
                irisOn = false;
            }
        }
    }
}
