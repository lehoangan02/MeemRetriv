import { z } from "zod";

const envSchema = z.object({
  MODE: z.string(),
  DEV: z.boolean(),
  PROD: z.boolean(),
  BASE_URL: z.string(),

  // Custom env
  VITE_NODE_ENV: z.string().default("development"),
  VITE_BACKEND_URL: z.url(),
  VITE_API_KEY: z.string().optional(),
});

export const ENV = envSchema.parse(import.meta.env);
