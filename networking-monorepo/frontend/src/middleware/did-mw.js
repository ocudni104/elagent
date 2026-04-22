import crypto from "node:crypto";

const DID_COOKIE_NAME = "did";
const DID_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 180;

function generateDid() {
  return crypto.randomUUID();
}

function isValidDid(value) {
  return (
    typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value)
  );
}

export function getDidCookieOptions(context) {
  return {
    httpOnly: true,
    sameSite: "lax",
    secure: context.url.protocol === "https:",
    path: "/",
    maxAge: DID_COOKIE_MAX_AGE_SECONDS
  };
}

export function didMiddleware(context, next) {
  let did = context.cookies.get(DID_COOKIE_NAME)?.value;

  if (!isValidDid(did)) {
    did = generateDid();
    context.cookies.set(DID_COOKIE_NAME, did, getDidCookieOptions(context));
  }

  context.locals.did = did;
  return next();
}
