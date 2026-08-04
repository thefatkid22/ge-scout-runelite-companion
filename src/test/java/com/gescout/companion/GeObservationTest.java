package com.gescout.companion;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class GeObservationTest
{
    @Test
    public void priceAndFillChangesProduceNewFingerprints()
    {
        GeObservation original = observation(1_000_000, 2, 2_000_000L);
        GeObservation repriced = observation(1_001_000, 2, 2_000_000L);
        GeObservation partialFill = observation(1_000_000, 3, 3_000_000L);

        assertNotEquals(original.fingerprint(), repriced.fingerprint());
        assertNotEquals(original.fingerprint(), partialFill.fingerprint());
    }

    private static GeObservation observation(
        int price,
        int filled,
        long amount)
    {
        return new GeObservation(
            "event-" + price + "-" + filled,
            "session",
            1,
            "offer_changed",
            "2026-07-29T12:00:00Z",
            0,
            4151,
            "Abyssal whip",
            "BUYING",
            "buy",
            price,
            10,
            filled,
            amount);
    }
}
