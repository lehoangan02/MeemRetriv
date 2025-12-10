export const themes = [
  "light",
  "dark",
  "lofi",
  "retro",
  "forest",
  "wireframe",
  "black",
  "sunset",
  "lemonade",
  "luxury",
  "valentine",
] as const;

export const themeLabels = themes.map(
  (theme) => theme.charAt(0).toUpperCase() + theme.slice(1),
);

export type Theme = (typeof themes)[number];
