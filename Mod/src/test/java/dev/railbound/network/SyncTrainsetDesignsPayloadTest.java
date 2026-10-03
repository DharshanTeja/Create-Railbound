package dev.railbound.network;

import dev.railbound.testutil.TestDesigns;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SyncTrainsetDesignsPayloadTest {

    private static SyncTrainsetDesignsPayload roundTrip(SyncTrainsetDesignsPayload payload) {
        ByteBuf buf = Unpooled.buffer();
        SyncTrainsetDesignsPayload.STREAM_CODEC.encode(buf, payload);
        return SyncTrainsetDesignsPayload.STREAM_CODEC.decode(buf);
    }

    @Test
    void designsRoundTrip() {
        var payload = new SyncTrainsetDesignsPayload(Map.of(
                ResourceLocation.fromNamespaceAndPath("railbound", "coach_standard"), TestDesigns.sample()));
        assertEquals(payload.designs(), roundTrip(payload).designs());
    }

    @Test
    void emptyMapRoundTrips() {
        assertEquals(Map.of(), roundTrip(new SyncTrainsetDesignsPayload(Map.of())).designs());
    }
}
