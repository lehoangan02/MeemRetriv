import { cn } from "@/lib/utils";
import { ImageOff, XIcon, FileImage, SearchIcon } from "lucide-react";
import React, { useMemo } from "react";

interface ImagePreviewProps {
  file: File | null;
  imageSrc: string | null;
  isLoading: boolean;
  onSearch: () => void;
  onRemove: () => void;
  onSelectImage: () => void;
}

export default function ImagePreview({
  file,
  imageSrc,
  isLoading,
  onRemove,
  onSearch,
  onSelectImage,
  className,
}: ImagePreviewProps & React.ComponentProps<"div">) {
  const fileName = file?.name;
  const fileSize = file?.size;
  const formattedSize = useMemo(() => {
    if (!fileSize) return null;
    if (fileSize < 1024) return `${fileSize} B`;
    if (fileSize < 1024 * 1024) return `${(fileSize / 1024).toFixed(1)} KB`;
    return `${(fileSize / (1024 * 1024)).toFixed(2)} MB`;
  }, [fileSize]);

  if (!imageSrc)
    return <EmptyImagePreview className={className} onClick={onSelectImage} />;

  return (
    <div
      className={cn(
        "group card bg-base-100",
        "shadow-2xl drop-shadow-2xl transition-all",
        className,
      )}
    >
      {onRemove && (
        <button
          title="Remove image"
          className="btn absolute top-0 right-0 z-10 btn-circle translate-x-1/3 -translate-y-1/3 shadow-md transition-transform btn-xs btn-error hover:scale-125"
          onClick={onRemove}
          disabled={isLoading}
        >
          <XIcon className="size-5" />
        </button>
      )}

      <figure className="relative h-full w-full overflow-hidden rounded-lg bg-base-300 p-4">
        <img
          src={imageSrc}
          alt="Preview image"
          className="z-1 size-auto max-h-full max-w-full object-contain shadow-primary"
        />
      </figure>

      {/* Footer */}
      {(fileName || formattedSize) && (
        <div className="card-body gap-1 p-4 pt-2">
          <div className="flex items-center justify-between gap-10">
            {/* Metadata */}
            <div className="flex items-center gap-3 overflow-hidden">
              <div className="flex items-center justify-center rounded-lg bg-primary p-2">
                <FileImage className="size-6 text-primary-content" />
              </div>
              <div className="flex flex-1 flex-col gap-1 overflow-hidden leading-none">
                {fileName && (
                  <p className="truncate font-medium" title={fileName}>
                    {fileName}
                  </p>
                )}
                {formattedSize && (
                  <p className="text-xs text-base-content/70">
                    {formattedSize}
                  </p>
                )}
              </div>
            </div>

            <button
              className="btn btn-circle btn-primary"
              onClick={onSearch}
              disabled={isLoading}
            >
              {isLoading ? (
                <span className="loading loading-spinner text-primary" />
              ) : (
                <SearchIcon className="size-5" />
              )}
            </button>
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
        "flex flex-col items-center justify-center gap-4",
        "max-w-4xl rounded-box py-16",
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
