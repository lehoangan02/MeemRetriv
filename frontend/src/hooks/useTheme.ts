import type { Themes } from "@/config/theme";

import { create } from "zustand";

type ThemeState = {
  theme: Themes;
  setTheme: (theme: Themes) => void;
};

const STORAGE_KEY = "theme";

export const useTheme = create<ThemeState>()((set) => {
  const savedTheme = localStorage.getItem(STORAGE_KEY) as Themes | null;
  const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
  const initialTheme = savedTheme || (isDark ? "dark" : "light");
  document.documentElement.setAttribute("data-theme", initialTheme);

  return {
    theme: initialTheme,
    setTheme: (theme) => {
      set({ theme });
      document.documentElement.setAttribute("data-theme", theme);
      localStorage.setItem(STORAGE_KEY, theme);
    },
  };
});
