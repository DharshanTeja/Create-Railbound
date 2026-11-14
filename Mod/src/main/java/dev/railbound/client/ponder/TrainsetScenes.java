package dev.railbound.client.ponder;

import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.railbound.Railbound;
import dev.railbound.trainset.item.TrainsetItem;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Scenes for the trainset item. The schematic is a 19 x 7 x 17 block stage: track along x at z = 8 (y = 1), a station
 * at (3, 1, 5) beside it, and one Standard Passenger Coach standing where Railbound puts the first carriage of an
 * assembly (its first block at x = 1, bogeys at x = 4 and x = 13, anchor at (9, 5, 8)).
 */
public final class TrainsetScenes {
    private TrainsetScenes() {}

    public static void placeTrain(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("place_train", "Placing a Trainset");
        scene.configureBasePlate(1, 0, 17);
        scene.scaleSceneView(.6f);
        scene.setSceneOffsetY(-1);
        scene.showBasePlate();
        scene.idle(5);

        for (int x = 18; x >= 0; x--) {
            scene.world().showSection(util.select().position(x, 1, 8), Direction.DOWN);
            scene.idle(1);
        }

        BlockPos stationPos = util.grid().at(3, 1, 5);
        Selection train = util.select().fromTo(1, 2, 7, 16, 5, 9);

        scene.world().showSection(util.select().position(stationPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
                .pointAt(util.vector().topOf(stationPos))
                .placeNearTarget()
                .attachKeyFrame()
                .text("Build a Create Train Station beside straight track and switch it to assembly mode");
        scene.idle(20);
        scene.world().cycleBlockProperty(stationPos, StationBlock.ASSEMBLING);
        scene.idle(70);

        ItemStack coach = TrainsetItem.of(Railbound.rl("coach_standard"));
        scene.overlay().showControls(util.vector().topOf(util.grid().at(9, 1, 8)), Pointing.DOWN, 50)
                .rightClick()
                .withItem(coach);
        scene.idle(25);
        ElementLink<WorldSectionElement> trainElement = scene.world().showIndependentSection(train, Direction.DOWN);
        scene.idle(25);

        scene.overlay().showText(90)
                .pointAt(util.vector().of(9.5, 5.5, 8.5))
                .placeNearTarget()
                .attachKeyFrame()
                .text("Right-click the assembly track with a trainset. The whole carriage appears, already on its bogeys");
        scene.idle(100);

        scene.overlay().showText(80)
                .pointAt(util.vector().of(14.5, 3.5, 8.5))
                .placeNearTarget()
                .text("Place more carriages behind it. Sneak-click to place one facing backwards");
        scene.idle(90);

        scene.world().cycleBlockProperty(stationPos, StationBlock.ASSEMBLING);
        scene.effects().indicateSuccess(stationPos);
        scene.world().animateTrainStation(stationPos, true);
        scene.overlay().showText(90)
                .pointAt(util.vector().topOf(stationPos))
                .placeNearTarget()
                .attachKeyFrame()
                .text("Press Assemble Train at the station. It is now a normal Create train: schedules, signals and stations all work");
        scene.idle(50);

        scene.world().animateTrainStation(stationPos, false);
        scene.world().moveSection(trainElement, util.vector().of(2, 0, 0), 40);
        scene.world().animateBogey(util.grid().at(4, 2, 8), 2f, 40);
        scene.world().animateBogey(util.grid().at(13, 2, 8), 2f, 40);
        scene.idle(60);
    }
}
