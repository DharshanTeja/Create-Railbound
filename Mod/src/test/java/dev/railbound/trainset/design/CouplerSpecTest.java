package dev.railbound.trainset.design;

import dev.railbound.testutil.TestDesigns;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CouplerSpecTest {

    @Test
    void theCoachHasAKnuckleCouplerAndAGangway() {
        CouplerSpec coupler = TestDesigns.sample().coupler().orElseThrow();
        assertEquals(-11, coupler.height());
        assertTrue(coupler.gangway().isPresent(), "coaches are walked through");
        assertEquals(List.of(), DesignValidator.validate(TestDesigns.sample()));
    }

    @Test
    void aDesignWithoutACouplerKeepsCreatesChain() {
        TrainsetDesign plain = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """);
        assertEquals(Optional.empty(), plain.coupler());
    }

    @Test
    void readsTheCouplerHeightAndTheGangwayOpening() {
        CouplerSpec coupler = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "coupler":{"height":-10,"gangway":{"half_width":11.5,"bottom":-9,"top":27}},
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """).coupler().orElseThrow();
        assertEquals(-10, coupler.height());
        assertEquals(Optional.of(new GangwaySpec(11.5, -9, 27)), coupler.gangway());
    }

    @Test
    void aLocoHasACouplerButNoGangway() {
        CouplerSpec coupler = TestDesigns.parse("""
                {"name":"n","category":"box_car","size":{"length":2,"width":1,"height":1},
                 "bogeys":[{"z":0},{"z":1}],
                 "coupler":{"height":-7.5},
                 "layout":{"palette":{"A":"anchor","#":"frame:floor"},"layers":[["A","#"]]}}
                """).coupler().orElseThrow();
        assertEquals(new CouplerSpec(-7.5, Optional.empty()), coupler);
    }

    @Test
    void aGangwayMustBeTallerThanItIsLow() {
        TrainsetDesign upsideDown = TestDesigns.withCoupler(TestDesigns.sample(),
                Optional.of(new CouplerSpec(-10, Optional.of(new GangwaySpec(11, 20, 10)))));
        assertTrue(DesignValidator.validate(upsideDown).stream().anyMatch(p -> p.contains("gangway top must be above its bottom")),
                () -> DesignValidator.validate(upsideDown).toString());
        TrainsetDesign tooWide = TestDesigns.withCoupler(TestDesigns.sample(),
                Optional.of(new CouplerSpec(-10, Optional.of(new GangwaySpec(30, -9, 27)))));
        assertTrue(DesignValidator.validate(tooWide).stream().anyMatch(p -> p.contains("gangway must fit within the carriage width")));
    }
}
