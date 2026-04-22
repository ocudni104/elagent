export const googleAuthActionPath = "/auth/google";

const transientCookieMaxAgeSeconds = 60 * 10;

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

export function getTransientCookieOptions(url) {
  return {
    httpOnly: true,
    sameSite: "lax",
    secure: url.protocol === "https:",
    path: "/",
    maxAge: transientCookieMaxAgeSeconds
  };
}

export function getGoogleAuthorizationUrl(env) {
  const backendUrl =
    env.INTERNAL_BACKEND_URL ?? env.PUBLIC_BACKEND_URL ?? "http://localhost:8080";

  return `${backendUrl}/api/idp/oauth2/authorization/google`;
}
