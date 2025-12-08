import { useTheme } from "@/hooks/useTheme";
import { MoonIcon, SunIcon } from "lucide-react";

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <label className="swap swap-rotate">
      <input
        type="checkbox"
        className="theme-controller"
        value="black"
        checked={theme === "black"}
        onChange={(e) => setTheme(e.target.checked ? "black" : "wireframe")}
      />
      <SunIcon className="swap-on size-8" />
      <MoonIcon className="swap-off size-8" />
    </label>
  );
}
