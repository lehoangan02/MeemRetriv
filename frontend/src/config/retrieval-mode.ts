export const retrievalModes = ["Text", "Image"] as const;
export type RetrievalMode = (typeof retrievalModes)[number];
