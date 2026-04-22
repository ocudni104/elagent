import type { APIRoute } from "astro";
import {
  getGoogleAuthorizationUrl,
  getTransientCookieOptions,
  sanitizeDeviceField
} from "../../lib/auth.js";

const redirectStatus = 303;

export const POST: APIRoute = async ({ request, cookies, redirect, url }) => {
  const formData = await request.formData();
  const deviceOs = sanitizeDeviceField(formData.get("deviceOs"), 120);
  const deviceScreen = sanitizeDeviceField(formData.get("deviceScreen"), 64);
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
