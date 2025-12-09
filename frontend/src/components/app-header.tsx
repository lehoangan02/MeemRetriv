import type React from "react";
import AppBranch from "@/components/app-branch";
import ThemeSelector from "./theme-selector";

export default function AppHeader({
  className,
  ...props
}: React.ComponentProps<"header">) {
  return (
    <header className={className} {...props}>
      <div className="mx-auto flex w-full max-w-base items-center justify-between px-4 py-2">
        <AppBranch />
        <ThemeSelector />
      </div>
    </header>
  );
}
