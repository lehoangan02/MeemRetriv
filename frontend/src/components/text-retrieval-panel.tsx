import { useRef } from "react";
import { cn } from "@/lib/utils";
import { Search, ArrowRight, BotIcon } from "lucide-react";

export default function TextRetrievalPanel() {
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const el = e.target;
    el.style.height = "auto";
    el.style.height = el.scrollHeight + "px";
  };

  const handleSearch = () => {
    const query = textareaRef.current?.value.trim();
    if (!query) return;
    alert(`Test query: ${query}`);
  };

  const handleSuggestionClick = (text: string) => {
    const el = textareaRef.current;
    if (el) {
      el.value = text;
      el.focus();
      el.style.height = "auto";
      el.style.height = el.scrollHeight + "px";
    }
  };

  return (
    <div className="flex min-h-full w-full flex-col items-center justify-center px-4 py-12">
      <div className="flex w-full max-w-3xl flex-col items-center gap-10">
        {/* Title & Slogan */}
        <div className="space-y-4 text-center">
          <h1 className="text-4xl font-extrabold tracking-tight transition-all md:text-5xl lg:text-6xl">
            Find the <span className="text-primary">perfect meme</span>
            <br /> for any moment.
          </h1>
          <p className="mx-auto max-w-xl text-sm leading-tight text-base-content/70 transition-all md:text-base lg:text-lg">
            Describe the emotion, the scene, or the vibe. Our AI retrieves the
            dankest results from the depths of the internet.
          </p>
        </div>

        {/*  Search Container */}
        <div className="group relative w-full">
          <div className="absolute -inset-2 rounded-lg bg-linear-to-r from-primary/50 via-secondary/50 to-primary/50 opacity-50 blur transition duration-500 group-hover:opacity-100" />

          <div className="relative flex w-full flex-col overflow-hidden rounded-lg bg-base-300 ring-1 ring-black/10 drop-shadow-xl transition-all focus-within:ring-primary/50 dark:ring-white/10">
            <textarea
              name="prompt"
              ref={textareaRef}
              onChange={handleInput}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  handleSearch();
                }
              }}
              className={cn(
                "textarea max-h-1000 min-h-36 w-full resize-none bg-transparent px-6 py-5",
                "border-none outline-none",
                "scrollbar-thin scrollbar-stable scrollbar-thumb-base-content/30 scrollbar-track-transparent",
                "text-base leading-normal text-base-content placeholder:text-base-content/30",
              )}
              placeholder="e.g., 'That cat nodding its head to music' or 'Coding error panic'..."
            />

            <div className="flex items-center justify-between border-t border-base-content/5 bg-base-300/50 px-4 py-3 backdrop-blur-sm">
              <div className="flex items-center gap-2 text-xs font-medium text-base-content/70">
                <BotIcon className="size-4 text-secondary" />
                <span>AI-Powered Search</span>
              </div>

              <button
                onClick={handleSearch}
                disabled={false}
                className={cn(
                  "btn gap-2 text-primary-content transition-all btn-sm btn-primary",
                )}
              >
                Search
                <ArrowRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Suggestion */}
        <div className="flex flex-wrap justify-center gap-2">
          <span className="py-1 text-sm text-base-content/60">
            Try searching:
          </span>
          {[
            "Coding struggles",
            "Awkward silence",
            "Reaction images",
            "Stupid Cat",
            "Fuck Dog",
            "Yellow Cat",
            "Trần Dần + Súng",
          ].map((tag) => (
            <button
              key={tag}
              onClick={() => handleSuggestionClick(tag)}
              className="badge cursor-pointer gap-1 badge-outline badge-lg transition-colors hover:bg-base-content/10 hover:text-base-content"
            >
              <Search className="size-4 opacity-70" />
              {tag}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
