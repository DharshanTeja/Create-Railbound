package dev.railbound.carriage;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import dev.railbound.registry.RailboundParticles;
import dev.railbound.steam.LocoGaugeCache;
import dev.railbound.steam.SteamGauges;
import dev.railbound.steam.Exhaust;
import dev.railbound.steam.TrainPower;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.SteamSpec;
import dev.railbound.trainset.load.TrainsetDesigns;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * On a moving train, rolls a loco's driving wheels by how far the carriage moved along its own length this tick, so
 * the motion turns in step with the train in either direction, and makes the exhaust: a chuff and a puff of smoke
 * from the chimney on every beat, idle wisps, and cylinder steam when starting off. Client side only.
 */
public class AnchorMovementBehaviour implements MovementBehaviour {
    @Override
    public void tick(MovementContext context) {
        if (!context.world.isClientSide()
                || !(context.contraption.getBlockEntityClientSide(context.localPos) instanceof AnchorBlockEntity anchor)) {
            return;
        }
        Optional<ParsedDesign> found = TrainsetDesigns.get(anchor.designId());
        if (found.isEmpty() || found.get().design().drive().isEmpty()) {
            return;
        }
        ParsedDesign design = found.get();
        Direction facing = context.state.getValue(AnchorBlock.FACING);
        Vec3 forward = context.rotation.apply(Vec3.atLowerCornerOf(facing.getNormal()));
        double distance = context.motion.dot(forward);
        anchor.roll(distance, design.design().drive().get().wheelRadius());
        anchor.noteHeading(forward, distance);

        // Create does not sync a carriage's storage to clients reliably: the server sends the gauges instead
        Optional<SteamGauges> gauges = LocoGaugeCache.get(context.contraption.entity.getId());
        boolean fireLit = gauges.map(SteamGauges::fireLit).orElse(false);
        int lumps = gauges.map(SteamGauges::lumps).orElse(0);
        boolean fired = anchor.noteSpeedAndCoal(Math.abs(distance), lumps);
        boolean accelerating = Math.abs(distance) > anchor.lastSpeed() + 1e-4;
        int chuffs = TrainPower.chuffs(Math.toDegrees(anchor.prevDriveAngle()), Math.toDegrees(anchor.driveAngle()));
        double pressure = gauges.map(SteamGauges::pressure).orElse(0.0);
        Exhaust exhaust = Exhaust.of(chuffs, fireLit, distance, accelerating, context.world.getGameTime(),
                anchor.ticksStopped(), pressure);
        boolean whistling = context.contraption.entity instanceof CarriageContraptionEntity cce && cce.getCarriage() != null
                && cce.getCarriage().train != null && cce.getCarriage().train.honkTicks > 0;
        design.design().steam().ifPresent(steam -> {
            blow(context, design, facing, steam, exhaust, fired, anchor.ticksStopped());
            if (whistling) {
                // a white plume from the whistle for as long as the horn sounds
                Vec3 whistle = world(context, design, facing, steam.whistle(), 1);
                Vec3 up = context.rotation.apply(new Vec3(0, 1, 0));
                RandomSource random = context.world.random;
                for (int i = 0; i < 2; i++) {
                    context.world.addParticle(RailboundParticles.LOCO_STEAM, whistle.x, whistle.y, whistle.z,
                            up.x * 0.25 + random.nextGaussian() * 0.02, up.y * 0.25, up.z * 0.25 + random.nextGaussian() * 0.02);
                }
            }
        });
    }

    private static void blow(MovementContext context, ParsedDesign design, Direction facing, SteamSpec steam,
                             Exhaust exhaust, boolean justFired, int ticksStopped) {
        Level level = context.world;
        RandomSource random = level.random;
        Vec3 up = context.rotation.apply(new Vec3(0, 1, 0));
        Vec3 right = context.rotation.apply(Vec3.atLowerCornerOf(facing.getClockWise().getNormal()));
        if (exhaust.smokePuffs() > 0 || exhaust.wisp()) {
            // coal smoke: dark, and darker still just after the fireman has put coal on
            Vec3 chimney = world(context, design, facing, steam.chimney(), 1);
            int puffs = exhaust.wisp() ? 1 : exhaust.smokePuffs();
            double speed = exhaust.wisp() ? 0.06 : 0.28;
            for (int i = 0; i < puffs; i++) {
                level.addParticle(justFired ? RailboundParticles.LOCO_SMOKE_DARK : RailboundParticles.LOCO_SMOKE,
                        chimney.x + random.nextGaussian() * 0.06, chimney.y, chimney.z + random.nextGaussian() * 0.06,
                        up.x * speed + random.nextGaussian() * 0.025, up.y * speed * (0.8 + random.nextFloat() * 0.4),
                        up.z * speed + random.nextGaussian() * 0.025);
            }
        }
        if (exhaust.chuff()) {
            Vec3 chimney = world(context, design, facing, steam.chimney(), 1);
            level.playLocalSound(chimney.x, chimney.y, chimney.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                    0.25f, 0.45f + random.nextFloat() * 0.1f, false);
        }
        if (exhaust.cylinderSteam() || exhaust.blowdown()) {
            // white steam from the drain cocks: a little while starting off, a strong burst when coming to a stand
            int bursts = exhaust.blowdown() ? 3 : 1;
            double force = exhaust.blowdown() ? 0.2 : 0.12;
            for (int side : new int[] {1, -1}) {
                Vec3 cock = world(context, design, facing, steam.cylinder(), side);
                for (int i = 0; i < bursts; i++) {
                    level.addParticle(RailboundParticles.LOCO_STEAM, cock.x, cock.y, cock.z,
                            right.x * side * force + random.nextGaussian() * 0.03, -0.01 + random.nextGaussian() * 0.01,
                            right.z * side * force + random.nextGaussian() * 0.03);
                }
            }
            if (exhaust.blowdown() && ticksStopped % 10 == 0) {
                Vec3 cock = world(context, design, facing, steam.cylinder(), 1);
                level.playLocalSound(cock.x, cock.y, cock.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                        0.5f, 0.7f + random.nextFloat() * 0.1f, false);
            }
        }
        if (exhaust.safetyValve()) {
            Vec3 valve = world(context, design, facing, steam.safetyValve(), 1);
            for (int i = 0; i < 2; i++) {
                level.addParticle(RailboundParticles.LOCO_STEAM, valve.x, valve.y, valve.z,
                        up.x * 0.35 + random.nextGaussian() * 0.02, up.y * 0.35, up.z * 0.35 + random.nextGaussian() * 0.02);
            }
            if (random.nextInt(8) == 0) {
                level.playLocalSound(valve.x, valve.y, valve.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL,
                        0.3f, 1.3f, false);
            }
        }
    }

    /** A model point (pixels; xSign mirrors it to the left side) in the world, riding the moving carriage. */
    private static Vec3 world(MovementContext context, ParsedDesign design, Direction facing, List<Double> point, int xSign) {
        double[] d = Exhaust.toDesign(point.get(0) * xSign, point.get(1), point.get(2), design.length());
        Vec3 local = CarriageTransform.toAnchorRelative(design, facing, d[0], d[1], d[2]).subtract(0.5, 0.5, 0.5);
        return context.position.add(context.rotation.apply(local));
    }
}
