package dev.railbound.trainset.item;

import dev.railbound.testutil.TestDesigns;
import dev.railbound.trainset.design.ParsedDesign;
import dev.railbound.trainset.design.TrainsetDesign;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TrainsetNamesTest {

    private static final ResourceLocation COACH = ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard");

    private static TranslatableContents contents(Component component) {
        return assertInstanceOf(TranslatableContents.class, component.getContents());
    }

    @Test
    void knownDesignUsesItsNameKey() {
        TrainsetDesign sample = TestDesigns.sample();
        Component name = TrainsetNames.displayName(COACH, id -> Optional.of(sample));
        assertEquals("trainset.railbound.coach_standard", contents(name).getKey());
    }

    @Test
    void unknownDesignShowsUnknownName() {
        Component name = TrainsetNames.displayName(COACH, id -> Optional.empty());
        TranslatableContents c = contents(name);
        assertEquals("item.railbound.trainset.unknown", c.getKey());
        assertEquals("railbound:coach_standard", c.getArgs()[0]);
    }

    @Test
    void missingComponentShowsEmptyName() {
        Component name = TrainsetNames.displayName(null, id -> Optional.empty());
        assertEquals("item.railbound.trainset.empty", contents(name).getKey());
    }

    @Test
    void tooltipListsCategoryLengthAndSeats() {
        List<Component> lines = TrainsetNames.tooltip(ParsedDesign.of(TestDesigns.sample()));
        assertEquals(3, lines.size());
        assertEquals("tooltip.railbound.category.passenger", contents(lines.get(0)).getKey());
        assertEquals("tooltip.railbound.length", contents(lines.get(1)).getKey());
        assertEquals(16, contents(lines.get(1)).getArgs()[0]);
        assertEquals("tooltip.railbound.seats", contents(lines.get(2)).getKey());
        assertEquals(20, contents(lines.get(2)).getArgs()[0]);
    }

    @Test
    void tooltipOmitsSeatsWhenThereAreNone() {
        TrainsetDesign boxCar = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}""");
        List<Component> lines = TrainsetNames.tooltip(ParsedDesign.of(boxCar));
        assertEquals(2, lines.size());
        assertEquals("tooltip.railbound.category.box_car", contents(lines.get(0)).getKey());
    }
}
