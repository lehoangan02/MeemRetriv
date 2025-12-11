import type React from "react";
import AppBranch from "@/components/app-branch";
import ModeSelector from "@/components/mode-selector";
import ThemeSelector from "@/components/theme-selector";

export default function AppHeader({
  className,
  ...props
}: React.ComponentProps<"header">) {
  return (
    <header className={className} {...props}>
      <div className="mx-auto flex w-full max-w-base items-center justify-between px-2 py-3">
        <AppBranch />
        <div className="flex items-center gap-3">
          <ModeSelector />
          <ThemeSelector />
        </div>
      </div>
    </header>
  );
}
