import { cn } from "@/lib/utils";
import { ImageOff, XIcon, FileImage } from "lucide-react";
import React, { useMemo } from "react";

interface ImagePreviewProps {
  imageSrc?: string | null;
  fileName?: string;
  fileSize?: number; // in bytes
  onRemove: () => void;
  onSelectImage: () => void;
}

export default function ImagePreview({
  imageSrc,
  fileName,
  fileSize,
  onRemove,
  onSelectImage,
}: ImagePreviewProps) {
  const formattedSize = useMemo(() => {
    if (!fileSize) return null;
    if (fileSize < 1024) return `${fileSize} B`;
    if (fileSize < 1024 * 1024) return `${(fileSize / 1024).toFixed(1)} KB`;
    return `${(fileSize / (1024 * 1024)).toFixed(2)} MB`;
  }, [fileSize]);

  if (!imageSrc) return <EmptyImagePreview onClick={onSelectImage} />;

  return (
    <div
      className={cn(
        "group card relative bg-base-100",
        "h-full w-full max-w-4xl",
        "shadow-2xl drop-shadow-2xl transition-all",
      )}
    >
      <figure className="relative h-full w-full overflow-hidden rounded-lg bg-base-300 p-4">
        <img
          src={imageSrc}
          alt="Preview image"
          className="z-1 h-auto w-full object-contain shadow-primary"
        />
      </figure>

      {onRemove && (
        <button
          title="Remove image"
          className="btn absolute top-0 right-0 z-10 btn-circle translate-x-1/3 -translate-y-1/3 shadow-md transition-transform btn-xs btn-error hover:scale-125"
          onClick={onRemove}
        >
          <XIcon className="size-5" />
        </button>
      )}

      {/* Footer / Metadata */}
      {(fileName || formattedSize) && (
        <div className="card-body gap-1 p-4 pt-2">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary">
              <FileImage className="size-6 text-primary-content" />
            </div>
            <div className="flex min-w-0 flex-1 flex-col">
              {fileName && (
                <p
                  className="truncate leading-tight font-medium"
                  title={fileName}
                >
                  {fileName}
                </p>
              )}
              {formattedSize && (
                <p className="text-xs text-base-content/70">{formattedSize}</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function EmptyImagePreview({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "h-full w-full max-w-4xl rounded-box py-16",
        "flex flex-col items-center justify-center gap-4",
        "border-4 border-dashed border-base-300 hover:bg-base-300",
        "cursor-pointer transition-all",
        className,
      )}
      {...props}
    >
      <div className="rounded-full bg-base-200 p-6">
        <ImageOff className="size-12 opacity-70" />
      </div>
      <div className="text-center text-base-content/70">
        <p className="text-lg font-bold">No image selected</p>
        <p className="text-base">Upload an image to get started</p>
      </div>
    </div>
  );
}
