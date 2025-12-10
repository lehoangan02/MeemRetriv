import { useTheme } from "@/hooks/useTheme";
import { ChevronDownIcon, PaletteIcon } from "lucide-react";
import { themeLabels, themes, type Theme } from "@/config/theme";

export default function ThemeSelector() {
  const { theme, setTheme } = useTheme();

  return (
    <div className="dropdown">
      <div tabIndex={0} role="button" className="btn btn-soft">
        <PaletteIcon className="size-5" />
        <p className="leading-none">Theme</p>
        <ChevronDownIcon className="inline-flex size-4 opacity-70" />
      </div>
      <ul
        tabIndex={-1}
        className="dropdown-content z-10 scrollbar-none max-h-60 min-w-full overflow-auto rounded-box bg-base-300 p-2 shadow-2xl"
      >
        {themes.map((it, idx) => {
          return (
            <li key={it}>
              <input
                type="radio"
                name="theme-dropdown"
                className="theme-controller btn btn-block justify-start btn-ghost btn-sm"
                value={it}
                checked={theme === it}
                onChange={(e) => {
                  setTheme(e.currentTarget.value as Theme);
                  e.currentTarget.blur();
                }}
                aria-label={themeLabels[idx]}
              />
            </li>
          );
        })}
      </ul>
    </div>
  );
}
