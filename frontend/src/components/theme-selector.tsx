import { useTheme } from "@/hooks/useTheme";
import { themeLabels, themes, type Themes } from "@/config/theme";

export default function ThemeSelector() {
  const { theme, setTheme } = useTheme();

  return (
    <div className="dropdown">
      <div tabIndex={0} role="button" className="btn btn-soft">
        Theme
        <svg
          width="12px"
          height="12px"
          className="inline-block h-2 w-2 fill-current opacity-60"
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 2048 2048"
        >
          <path d="M1799 349l242 241-1017 1017L7 590l242-241 775 775 775-775z"></path>
        </svg>
      </div>
      <ul
        tabIndex={-1}
        className="dropdown-content z-1 w-52 rounded-box bg-base-300 p-2 shadow-2xl"
      >
        {themes.map((it, idx) => {
          return (
            <li key={it}>
              <input
                type="radio"
                name="theme-dropdown"
                className="theme-controller btn btn-block w-full justify-start btn-ghost btn-sm"
                value={it}
                checked={theme === it}
                onChange={(e) => setTheme(e.currentTarget.value as Themes)}
                aria-label={themeLabels[idx]}
              />
            </li>
          );
        })}
      </ul>
    </div>
  );
}
