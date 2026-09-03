package com.gescout.companion;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class GeScoutSyncClient
{
    private static final MediaType JSON =
        MediaType.parse("application/json; charset=utf-8");
    private static final String USER_AGENT =
        "GE-Scout-RuneLite-Companion/0.2.0";
    private static final int MAX_QUEUE = 256;
    private static final int MAX_BATCH = 64;
    private static final Type QUEUE_TYPE =
        new TypeToken<List<GeObservation>>() { }.getType();

    private final OkHttpClient http;
    private final Gson gson;
    private final ConfigManager configManager;
    private final GeScoutCompanionConfig config;
    private final List<GeObservation> queue = new ArrayList<>();
    private Consumer<String> statusListener = ignored -> { };

    @Inject
    GeScoutSyncClient(
        OkHttpClient http,
        Gson gson,
        ConfigManager configManager,
        GeScoutCompanionConfig config)
    {
        this.http = http;
        this.gson = gson;
        this.configManager = configManager;
        this.config = config;
    }

    synchronized void start(Consumer<String> listener)
    {
        statusListener = listener;
        try
        {
            List<GeObservation> stored =
                gson.fromJson(config.eventQueue(), QUEUE_TYPE);
            if (stored != null)
            {
                queue.clear();
                queue.addAll(stored.subList(
                    Math.max(0, stored.size() - MAX_QUEUE),
                    stored.size()));
            }
        }
        catch (RuntimeException ignored)
        {
            queue.clear();
            persist();
        }
        report(queue.isEmpty() ? "Ready" : queue.size() + " update(s) queued");
    }

    synchronized void enqueue(GeObservation observation)
    {
        queue.add(observation);
        while (queue.size() > MAX_QUEUE)
        {
            queue.remove(0);
        }
        persist();
        report(queue.size() + " update(s) queued");
    }

    synchronized int queuedCount()
    {
        return queue.size();
    }

    void pairIfNeeded()
    {
        String code = config.pairingCode().trim();
        if (code.isEmpty() || !config.companionToken().trim().isEmpty())
        {
            return;
        }
        try
        {
            URI base = ServerAddressPolicy.requireSafeBaseUrl(config.serverUrl());
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            body.addProperty("deviceName", config.deviceName().trim());
            Request request = request(
                base.resolve("/api/sync/pair"),
                gson.toJson(body),
                null);
            try (Response response = http.newCall(request).execute())
            {
                String payload = response.body() == null
                    ? ""
                    : response.body().string();
                if (!response.isSuccessful())
                {
                    throw new IllegalStateException(errorMessage(payload));
                }
                JsonObject result =
                    new JsonParser().parse(payload).getAsJsonObject();
                String token = result.get("token").getAsString();
                configManager.setConfiguration(
                    GeScoutCompanionConfig.GROUP,
                    "companionToken",
                    token);
                configManager.setConfiguration(
                    GeScoutCompanionConfig.GROUP,
                    "pairingCode",
                    "");
                report("Paired with GE Scout");
            }
        }
        catch (Exception error)
        {
            report("Pairing failed: " + shortMessage(error));
        }
    }

    void flush()
    {
        List<GeObservation> batch;
        synchronized (this)
        {
            if (queue.isEmpty() || config.companionToken().trim().isEmpty())
            {
                return;
            }
            batch = new ArrayList<>(
                queue.subList(0, Math.min(MAX_BATCH, queue.size())));
        }
        try
        {
            URI base = ServerAddressPolicy.requireSafeBaseUrl(config.serverUrl());
            JsonObject body = new JsonObject();
            body.add("events", gson.toJsonTree(batch));
            Request request = request(
                base.resolve("/api/sync/events"),
                gson.toJson(body),
                config.companionToken().trim());
            try (Response response = http.newCall(request).execute())
            {
                String payload = response.body() == null
                    ? ""
                    : response.body().string();
                if (!response.isSuccessful())
                {
                    throw new IllegalStateException(errorMessage(payload));
                }
            }
            synchronized (this)
            {
                for (GeObservation sent : batch)
                {
                    queue.remove(sent);
                }
                persist();
                report(queue.isEmpty()
                    ? "Synced just now"
                    : queue.size() + " update(s) queued");
            }
        }
        catch (Exception error)
        {
            report("Offline; " + queuedCount() + " queued · " + shortMessage(error));
        }
    }

    String fetchPlans()
    {
        if (config.companionToken().trim().isEmpty())
        {
            return "Pair this client from GE Scout Settings.";
        }
        try
        {
            URI base = ServerAddressPolicy.requireSafeBaseUrl(config.serverUrl());
            Request request = requestBuilder(
                base.resolve("/api/sync/companion"))
                .header("Authorization", "Bearer " + config.companionToken().trim())
                .get()
                .build();
            try (Response response = http.newCall(request).execute())
            {
                String payload = response.body() == null
                    ? ""
                    : response.body().string();
                if (!response.isSuccessful())
                {
                    throw new IllegalStateException(errorMessage(payload));
                }
                JsonObject companion = new JsonParser()
                    .parse(payload)
                    .getAsJsonObject();
                JsonArray plans = companion.getAsJsonArray("plans");
                JsonArray alerts = companion.getAsJsonArray("alerts");
                StringBuilder text = new StringBuilder();
                if (plans != null)
                {
                    for (JsonElement value : plans)
                    {
                        JsonObject plan = value.getAsJsonObject();
                        if (text.length() > 0)
                        {
                            text.append("\n\n");
                        }
                        text.append(plan.get("itemName").getAsString())
                            .append("\n")
                            .append(plan.get("stage").getAsString())
                            .append(" · ")
                            .append(plan.get("boughtQuantity").getAsInt())
                            .append(" bought · ")
                            .append(plan.get("completedQuantity").getAsInt())
                            .append(" completed");
                    }
                }
                if (alerts != null && alerts.size() > 0)
                {
                    text.append(text.length() > 0 ? "\n\nAlerts\n" : "Alerts\n");
                    int count = Math.min(3, alerts.size());
                    for (int index = 0; index < count; index++)
                    {
                        JsonObject alert = alerts.get(index).getAsJsonObject();
                        text.append("\n• ")
                            .append(alert.get("itemName").getAsString())
                            .append(": ")
                            .append(alert.get("message").getAsString());
                    }
                }
                return text.length() == 0
                    ? "No active GE Scout offers or alerts."
                    : text.toString();
            }
        }
        catch (Exception error)
        {
            return "Plans unavailable: " + shortMessage(error);
        }
    }

    private Request request(URI url, String json, String token)
    {
        Request.Builder builder = requestBuilder(url)
            .post(RequestBody.create(JSON, json));
        if (token != null)
        {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    static Request.Builder requestBuilder(URI url)
    {
        return new Request.Builder()
            .url(url.toString())
            .header("User-Agent", USER_AGENT);
    }

    private String errorMessage(String payload)
    {
        try
        {
            JsonObject value = new JsonParser().parse(payload).getAsJsonObject();
            return value.has("error") ? value.get("error").getAsString() : payload;
        }
        catch (RuntimeException ignored)
        {
            return payload.isEmpty() ? "server request failed" : payload;
        }
    }

    private synchronized void persist()
    {
        configManager.setConfiguration(
            GeScoutCompanionConfig.GROUP,
            "eventQueue",
            gson.toJson(Collections.unmodifiableList(queue)));
    }

    private void report(String value)
    {
        statusListener.accept(value);
    }

    private String shortMessage(Exception error)
    {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
            ? error.getClass().getSimpleName()
            : message;
    }
}
