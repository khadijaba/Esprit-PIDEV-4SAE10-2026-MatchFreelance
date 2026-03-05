// Copy this file to your Jitsi Docker web config directory, e.g.:
//   ~/.jitsi-meet-cfg/web/custom-config.js   (Linux/Mac)
//   %USERPROFILE%\.jitsi-meet-cfg\web\custom-config.js   (Windows)
// Then: docker compose restart web
//
// This allows any participant to start the meeting (no "waiting for moderator" on meet.jit.si).
config.everyoneIsModerator = true;
