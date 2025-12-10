import { create } from "zustand";
import type { RetrievalMode } from "@/config/retrieval-mode";

type RetrievalModeState = {
  mode: RetrievalMode;
  setMode: (mode: RetrievalMode) => void;
};

const STORAGE_KEY = "retrieval-mode";

export const useRetrievalMode = create<RetrievalModeState>()((set) => {
  const savedMode = localStorage.getItem(STORAGE_KEY) as RetrievalMode | null;
  const initialMode = savedMode || "Image";

  return {
    mode: initialMode,
    setMode: (mode) => {
      set({ mode });
      localStorage.setItem(STORAGE_KEY, mode);
    },
  };
});
