import type { APIRoute } from "astro";
import {
  getGoogleAuthorizationUrl,
  getTransientCookieOptions,
  sanitizeDeviceField
} from "../../lib/auth.js";

export const prerender = false;

const redirectStatus = 303;

function canReadFormData(request: Request) {
  const contentType = request.headers.get("content-type") ?? "";

  return (
    contentType.includes("application/x-www-form-urlencoded") ||
    contentType.includes("multipart/form-data")
  );
}

export const POST: APIRoute = async ({ request, cookies, redirect, url }) => {
  const formData = canReadFormData(request) ? await request.formData() : null;
  const deviceOs = sanitizeDeviceField(formData?.get("deviceOs") ?? null, 120);
  const deviceScreen = sanitizeDeviceField(formData?.get("deviceScreen") ?? null, 64);
  const cookieOptions = getTransientCookieOptions(url);

  if (deviceOs) {
    cookies.set("device_os", deviceOs, cookieOptions);
  }

  if (deviceScreen) {
    cookies.set("device_screen", deviceScreen, cookieOptions);
  }

  return redirect(getGoogleAuthorizationUrl(import.meta.env), redirectStatus);
};

export const GET: APIRoute = ({ redirect }) => {
  return redirect("/login", 302);
};
