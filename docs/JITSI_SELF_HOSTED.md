# Self-hosted Jitsi (fix "waiting for moderator")

The public **meet.jit.si** server does **not** allow `everyoneIsModerator`, so participants see:

> "The conference has not yet started because no moderators have yet arrived. If you'd like to become a moderator please log-in. Otherwise, please wait."

To fix this, use a **self-hosted Jitsi** instance where you can set `everyoneIsModerator: true`.

## 1. Run Jitsi with Docker

Follow the [official Jitsi Docker guide](https://jitsi.github.io/handbook/docs/devops-guide/devops-guide-docker):

1. Download the [latest release](https://github.com/jitsi/docker-jitsi-meet/releases/latest) (zip), unzip it.
2. Copy `env.example` to `.env` and set passwords (run `./gen-passwords.sh` on Linux/Mac).
3. Create config dirs, e.g.:
   ```bash
   mkdir -p ~/.jitsi-meet-cfg/web
   ```
4. Add the custom config so everyone is moderator (see below).
5. Run: `docker compose up -d`
6. Open https://localhost:8443 (accept the self-signed certificate).

## 2. Enable "everyone is moderator"

Create a file **custom-config.js** in the Jitsi web config directory.  
On Docker this is the `CONFIG` directory from `.env` (e.g. `~/.jitsi-meet-cfg`), subfolder **web**:

- **Path:** `~/.jitsi-meet-cfg/web/custom-config.js` (Linux/Mac)  
  or `%USERPROFILE%\.jitsi-meet-cfg\web\custom-config.js` (Windows).

**Content of `custom-config.js`:**

```javascript
// Allow any participant to start the meeting (no "waiting for moderator")
config.everyoneIsModerator = true;
```

Then restart the web container so it picks up the file:

```bash
docker compose restart web
```

## 3. Point the app to your Jitsi server

Set the Jitsi base URL in the **interview-service**:

- **Option A – `application.properties`** (in `interview-service/src/main/resources/`):
  ```properties
  jitsi.base-url=https://localhost:8443
  ```
  (Use the URL you use in the browser, including port if not 443.)

- **Option B – environment variable:**
  ```bash
  export JITSI_BASE_URL=https://localhost:8443
  ```
  and in `application.properties`:
  ```properties
  jitsi.base-url=${JITSI_BASE_URL:https://meet.jit.si}
  ```
  (Spring allows env override if you add the property with that syntax.)

Restart the **interview-service** after changing the config.

## 4. Frontend / CORS

The Angular app runs on `http://localhost:4200` and will load the Jitsi script and iframe from your Jitsi base URL (e.g. `https://localhost:8443`). If your Jitsi is on another port or domain, ensure:

- You open the app over **HTTPS** if Jitsi is HTTPS (or use the same scheme), and
- The browser allows the iframe (no strict X-Frame-Options blocking).

For local development, using `https://localhost:8443` as `jitsi.base-url` is usually enough.

## Summary

| Step | Action |
|------|--------|
| 1 | Run Jitsi via Docker (official guide). |
| 2 | Add `custom-config.js` with `config.everyoneIsModerator = true` in the web config dir; restart `web` container. |
| 3 | Set `jitsi.base-url` in interview-service to your Jitsi URL (e.g. `https://localhost:8443`). |
| 4 | Restart interview-service and use the app; the "waiting for moderator" message should disappear. |
