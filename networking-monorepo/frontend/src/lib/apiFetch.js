// src/lib/api-fetch.js
export async function apiFetch(context, url, options = {}) {
  const headers = new Headers(options.headers || {});

  if (context.locals?.did) {
    headers.set("X-Device-Id", context.locals.did);
  }

  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    ...options,
    credentials: "include",
    headers
  });

  return response;
}
