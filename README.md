# GE Scout RuneLite companion

This is a read-only RuneLite Plugin Hub-style project. It observes the eight
Grand Exchange slots, queues snapshots and offer-change events, and sends them
to a paired self-hosted GE Scout server. It never invokes game actions,
generates input, or places offers.

## Pair it

1. Open GE Scout on a device that can reach the same LAN or Tailscale server.
2. In **Settings → RuneLite companion**, create a one-time code.
3. In RuneLite, open **GE Scout Companion** settings.
4. Enter the same GE Scout server URL and pairing code.
5. Enable the plugin. The token is stored in RuneLite configuration after the
   code is exchanged, and the code is cleared.

Plain HTTP is accepted only for loopback, LAN, and Tailscale addresses. Use
HTTPS for any other hostname. A first login snapshot is deliberately sent to
GE Scout's review inbox.

## Develop

This project follows RuneLite's official example-plugin layout and emits
Java 11-compatible bytecode. Use JDK 17 or newer to run Gradle:

```text
gradle clean test --no-daemon
gradle run --no-daemon
```

CI installs Gradle 8.11 explicitly, compiles the plugin, and runs its unit
tests on every push and pull request. The Gradle `run` task launches a
developer-mode client with this plugin. A Plugin Hub submission still requires
manual RuneLite testing, RuneLite review, and the repository metadata expected
by the Plugin Hub.

The local queue is capped at 256 observations. Failed requests remain queued;
the server's event IDs make retries idempotent.

## Release checklist

- CI passes against the current RuneLite client.
- Pairing works over loopback, LAN, Tailscale, and private HTTPS.
- Login sends exactly one review-only eight-slot snapshot.
- Partial fills, repricing, cancellations, and completions appear in GE Scout.
- Restarting RuneLite drains the persisted queue without duplicate outcomes.
- The panel shows current GE Scout plans and recent alerts.
- No code invokes client menu actions, mouse input, keyboard input, or offer
  placement.
- The Plugin Hub manifest version and repository commit are ready for review.
