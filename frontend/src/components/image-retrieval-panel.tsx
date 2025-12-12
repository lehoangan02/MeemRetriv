import { useEffect, useRef, useState } from "react";
import ImagePreview from "@/components/image-preview";
import InputFileButton from "@/components/input-file-button";
import useRetrieve from "@/hooks/useRetrieve";
import { useImageHistory } from "@/hooks/useImageHistory";

export default function ImageRetrievalPanel() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const { mutate: retrieve, isPending } = useRetrieve();
  const { append } = useImageHistory();

  function handleFileChange(newFile: File | null) {
    setFile(newFile);
    if (newFile) {
      const url = URL.createObjectURL(newFile);
      setPreviewUrl(url);
      append(newFile);
    } else {
      setPreviewUrl(null);
    }
  }

  function handleSearch() {
    if (!file) return;
    retrieve({ file: file });
  }

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  return (
    <div className="flex h-full flex-col items-center gap-10 px-4 py-8">
      <ImagePreview
        file={file}
        imageSrc={previewUrl}
        isLoading={isPending}
        onRemove={() => handleFileChange(null)}
        onSearch={handleSearch}
        onSelectImage={() => inputRef.current?.click()}
        className="min-h-0 w-full max-w-4xl flex-1"
      />
      <InputFileButton
        inputRef={inputRef}
        onFileSelected={(file) => handleFileChange(file)}
      />
    </div>
  );
}
