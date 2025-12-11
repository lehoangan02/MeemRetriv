import type { ComponentProps } from "react";

import { useRetrievalMode } from "@/hooks/useRetrivalMode";
import TextRetrievalPanel from "@/components/text-retrieval-panel";
import ImageRetrievalPanel from "@/components/image-retrieval-panel";

export default function MainContent({
  className,
  ...props
}: ComponentProps<"main">) {
  const mode = useRetrievalMode((state) => state.mode);

  return (
    <main className={className} {...props}>
      <div className="h-full flex-1">
        {mode === "Text" && <TextRetrievalPanel />}
        {mode === "Image" && <ImageRetrievalPanel />}
      </div>
    </main>
  );
}
