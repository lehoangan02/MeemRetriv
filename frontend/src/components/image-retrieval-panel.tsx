import { useEffect, useMemo, useRef, useState } from "react";
import ImagePreview from "@/components/image-preview";
import InputFileButton from "@/components/input-file-button";
import useRetrieve from "@/hooks/useRetrieve";
import { useImageHistory } from "@/hooks/useImageHistory";
import MemeSearchResults from "./meme-search-results";

export default function ImageRetrievalPanel() {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const { mutate: retrieve, isPending, data } = useRetrieve();
  const memes = useMemo(() => {
    if (!data) return [];
    return data.map((it) => `data:image/jpeg;base64,${it.imageBase64}`);
  }, [data]);
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
    <div className="h-full w-full">
      <div className="flex h-11/12 flex-col items-center gap-10 px-4 py-8">
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
      <MemeSearchResults memes={memes} />
    </div>
  );
}
