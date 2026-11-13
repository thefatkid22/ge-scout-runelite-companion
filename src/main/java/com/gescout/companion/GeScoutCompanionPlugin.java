package com.gescout.companion;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@PluginDescriptor(
    name = "GE Scout Companion",
    description = "Read-only Grand Exchange observation sync for GE Scout",
    tags = {"grand exchange", "ge", "offers", "sync", "mobile"}
)
public class GeScoutCompanionPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ClientToolbar toolbar;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private ClientThread clientThread;

    @Inject
    private GeScoutSyncClient syncClient;

    private final Map<Integer, String> slotFingerprints = new HashMap<>();
    private GeScoutPanel panel;
    private NavigationButton navigation;
    private ScheduledFuture<?> syncTask;
    private ScheduledFuture<?> planTask;
    private ScheduledFuture<?> snapshotTask;
    private String sessionId = UUID.randomUUID().toString();
    private long sequence;
    private boolean snapshotQueued;

    @Provides
    GeScoutCompanionConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GeScoutCompanionConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new GeScoutPanel();
        navigation = NavigationButton.builder()
            .tooltip("GE Scout Companion")
            .icon(icon())
            .priority(8)
            .panel(panel)
            .build();
        toolbar.addNavigation(navigation);
        syncClient.start(panel::setStatus);
        syncTask = executor.scheduleWithFixedDelay(() ->
        {
            syncClient.pairIfNeeded();
            syncClient.flush();
        }, 0, 2, TimeUnit.SECONDS);
        planTask = executor.scheduleWithFixedDelay(
            () -> panel.setPlans(syncClient.fetchPlans()),
            1,
            30,
            TimeUnit.SECONDS);
        snapshotTask = executor.scheduleWithFixedDelay(
            () -> clientThread.invokeLater(this::queueSafetySnapshot),
            5,
            15,
            TimeUnit.SECONDS);
    }

    @Override
    protected void shutDown()
    {
        if (syncTask != null)
        {
            syncTask.cancel(false);
        }
        if (planTask != null)
        {
            planTask.cancel(false);
        }
        if (snapshotTask != null)
        {
            snapshotTask.cancel(false);
        }
        if (navigation != null)
        {
            toolbar.removeNavigation(navigation);
        }
        slotFingerprints.clear();
        snapshotQueued = false;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (GeScoutCompanionConfig.GROUP.equals(event.getGroup()))
        {
            executor.execute(syncClient::pairIfNeeded);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            snapshotQueued = false;
            slotFingerprints.clear();
            return;
        }
        if (event.getGameState() != GameState.LOGGED_IN || snapshotQueued)
        {
            return;
        }
        snapshotQueued = true;
        sessionId = UUID.randomUUID().toString();
        sequence = 0;
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        for (int slot = 0; slot < offers.length; slot++)
        {
            enqueue(offers[slot], slot, "snapshot", true);
        }
    }

    @Subscribe
    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
    {
        if (!snapshotQueued)
        {
            return;
        }
        enqueue(event.getOffer(), event.getSlot(), "offer_changed", false);
    }

    private void enqueue(
        GrandExchangeOffer offer,
        int slot,
        String eventType,
        boolean snapshot)
    {
        GeObservation observation = observation(offer, slot, eventType);
        String fingerprint = observation.fingerprint();
        if (!snapshot && fingerprint.equals(slotFingerprints.get(slot)))
        {
            return;
        }
        slotFingerprints.put(slot, fingerprint);
        syncClient.enqueue(observation);
        panel.setObservation(
            slot,
            observation.state,
            observation.itemName,
            observation.filledQuantity,
            observation.totalQuantity,
            observation.offerPrice,
            eventType);
    }

    private void queueSafetySnapshot()
    {
        if (client.getGameState() != GameState.LOGGED_IN || !snapshotQueued)
        {
            return;
        }
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        for (int slot = 0; slot < offers.length; slot++)
        {
            enqueue(offers[slot], slot, "snapshot", false);
        }
    }

    private GeObservation observation(
        GrandExchangeOffer offer,
        int slot,
        String eventType)
    {
        GrandExchangeOfferState state = offer.getState();
        int rawItemId = offer.getItemId();
        Integer itemId =
            state == GrandExchangeOfferState.EMPTY || rawItemId <= 0
                ? null
                : rawItemId;
        String itemName = itemId == null
            ? null
            : itemManager.getItemComposition(itemId).getName();
        return new GeObservation(
            UUID.randomUUID().toString(),
            sessionId,
            sequence++,
            eventType,
            Instant.now().toString(),
            slot,
            itemId,
            itemName,
            state.name(),
            direction(state),
            safeOfferPrice(offer.getPrice()),
            Math.max(0, offer.getTotalQuantity()),
            Math.max(0, offer.getQuantitySold()),
            Math.max(0L, (long) offer.getSpent()));
    }

    static int safeOfferPrice(long price)
    {
        return (int) Math.max(
            0L,
            Math.min((long) Integer.MAX_VALUE, price));
    }

    static String direction(GrandExchangeOfferState state)
    {
        switch (state)
        {
            case BUYING:
            case BOUGHT:
            case CANCELLED_BUY:
                return "buy";
            case SELLING:
            case SOLD:
            case CANCELLED_SELL:
                return "sell";
            default:
                return null;
        }
    }

    private static BufferedImage icon()
    {
        BufferedImage image =
            new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(19, 23, 19));
        graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
        graphics.setColor(new Color(208, 164, 71));
        graphics.drawLine(4, 11, 7, 8);
        graphics.drawLine(7, 8, 9, 10);
        graphics.drawLine(9, 10, 13, 5);
        graphics.dispose();
        return image;
    }
}
