import { defineMiddleware } from "astro:middleware";
import { didMiddleware } from "./middleware/did-mw.js";

export const onRequest = defineMiddleware((context, next) => {
  return didMiddleware(context, next);
});
