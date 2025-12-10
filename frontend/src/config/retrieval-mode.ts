export const retrievalModes = ["Image", "Text"] as const;
export type RetrievalMode = (typeof retrievalModes)[number];
