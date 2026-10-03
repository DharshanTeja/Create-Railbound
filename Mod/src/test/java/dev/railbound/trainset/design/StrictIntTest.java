package dev.railbound.trainset.design;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StrictIntTest {

    private static boolean decodes(String sizeJson) {
        return CarriageSize.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(sizeJson)).result().isPresent();
    }

    @Test
    void acceptsWholeNumbers() {
        assertTrue(decodes("{\"length\":16,\"width\":3,\"height\":3}"));
    }

    @Test
    void rejectsFractionalNumbers() {
        assertFalse(decodes("{\"length\":16.5,\"width\":3,\"height\":3}"));
    }

    @Test
    void rejectsZeroLength() {
        assertFalse(decodes("{\"length\":0,\"width\":3,\"height\":3}"));
    }

    @Test
    void rejectsNegativeCargo() {
        assertTrue(CargoSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"item_slots\":-1}")).result().isEmpty());
    }

    @Test
    void bogeyZMustBeWhole() {
        assertTrue(BogeySpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"z\":3.7}")).result().isEmpty());
    }
}
