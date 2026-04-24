export const googleAuthActionPath = "/auth/google";

export function sanitizeDeviceField(value, maxLength) {
  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim().replace(/\s+/g, " ");
  if (!normalized) {
    return null;
  }

  return normalized.slice(0, maxLength);
}

export function getGoogleAuthorizationUrl(env, device = {}) {
  const backendUrl =
    env.PUBLIC_BACKEND_URL ?? env.INTERNAL_BACKEND_URL ?? "http://localhost:8080";
  const url = new URL("/api/idp/oauth2/authorization/google", backendUrl);

  if (device.deviceOs) {
    url.searchParams.set("deviceOs", device.deviceOs);
  }

  if (device.deviceScreen) {
    url.searchParams.set("deviceScreen", device.deviceScreen);
  }

  return url.toString();
}
