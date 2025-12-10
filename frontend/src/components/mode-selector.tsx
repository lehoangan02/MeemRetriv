import { retrievalModes, type RetrievalMode } from "@/config/retrieval-mode";
import { useRetrievalMode } from "@/hooks/useRetrivalMode";
import { Settings2Icon } from "lucide-react";

export default function ModeSelector() {
  const { mode, setMode } = useRetrievalMode();

  return (
    <div className="dropdown">
      <div tabIndex={0} role="button" className="btn min-w-36 btn-soft">
        <Settings2Icon className="size-5" />
        <p className="leading-none">
          Mode: <span>{mode}</span>
        </p>
      </div>
      <ul
        tabIndex={-1}
        className="dropdown-content z-10 scrollbar-none max-h-60 min-w-full overflow-auto rounded-box bg-base-300 p-2 shadow-2xl"
      >
        {retrievalModes.map((it) => {
          return (
            <li key={it}>
              <input
                type="radio"
                name="mode-dropdown"
                className="btn btn-block justify-start btn-ghost btn-sm"
                value={it}
                checked={mode === it}
                onChange={(e) => {
                  setMode(e.currentTarget.value as RetrievalMode);
                  e.currentTarget.blur();
                }}
                aria-label={it}
              />
            </li>
          );
        })}
      </ul>
    </div>
  );
}
