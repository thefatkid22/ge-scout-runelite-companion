package com.gescout.companion;

import java.util.Objects;

final class GeObservation
{
    final int schemaVersion = 1;
    final String eventId;
    final String sessionId;
    final long sequence;
    final String eventType;
    final String capturedAt;
    final int slot;
    final Integer itemId;
    final String itemName;
    final String state;
    final String direction;
    final int offerPrice;
    final int totalQuantity;
    final int filledQuantity;
    final long amountExchanged;

    GeObservation(
        String eventId,
        String sessionId,
        long sequence,
        String eventType,
        String capturedAt,
        int slot,
        Integer itemId,
        String itemName,
        String state,
        String direction,
        int offerPrice,
        int totalQuantity,
        int filledQuantity,
        long amountExchanged)
    {
        this.eventId = eventId;
        this.sessionId = sessionId;
        this.sequence = sequence;
        this.eventType = eventType;
        this.capturedAt = capturedAt;
        this.slot = slot;
        this.itemId = itemId;
        this.itemName = itemName;
        this.state = state;
        this.direction = direction;
        this.offerPrice = offerPrice;
        this.totalQuantity = totalQuantity;
        this.filledQuantity = filledQuantity;
        this.amountExchanged = amountExchanged;
    }

    String fingerprint()
    {
        return slot + ":" + itemId + ":" + state + ":" + offerPrice + ":"
            + totalQuantity + ":" + filledQuantity + ":" + amountExchanged;
    }

    @Override
    public boolean equals(Object value)
    {
        if (!(value instanceof GeObservation))
        {
            return false;
        }
        GeObservation other = (GeObservation) value;
        return Objects.equals(eventId, other.eventId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(eventId);
    }
}
