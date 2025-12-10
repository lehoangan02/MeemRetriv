import { useEffect, useRef, useState } from "react";
import ImagePreview from "@/components/image-preview";
import InputFileButton from "@/components/input-file-button";

export default function ImageUploader() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  function handleFileChange(newFile: File | null) {
    setFile(newFile);
    if (newFile) {
      const url = URL.createObjectURL(newFile);
      setPreviewUrl(url);
    } else {
      setPreviewUrl(null);
    }
  }

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  return (
    <div className="flex h-full flex-col items-center gap-10 px-4 py-8">
      <ImagePreview
        imageSrc={previewUrl}
        fileName={file?.name}
        fileSize={file?.size}
        onRemove={() => handleFileChange(null)}
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
