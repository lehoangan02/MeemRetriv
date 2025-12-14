import { useState } from "react";
import ImageModal from "./image-modal";

type MemeSearchResultsProps = {
  memes: string[];
} & React.ComponentProps<"div">;

export default function MemeSearchResults({ memes }: MemeSearchResultsProps) {
  const [modalImage, setModalImage] = useState<string | null>(null);

  if (!memes) return null;

  return (
    <div className="w-full px-4 py-6">
      <div className="columns-1 sm:columns-2 lg:columns-3 xl:columns-4 2xl:columns-5">
        {memes.map((meme, idx) => {
          return (
            <img
              key={idx}
              src={meme}
              alt={`Meme ${idx + 1}`}
              className="mb-1 w-full cursor-pointer"
              onClick={() => setModalImage(meme)}
            />
          );
        })}
      </div>
      {modalImage && (
        <ImageModal src={modalImage} onClose={() => setModalImage(null)} />
      )}
    </div>
  );
}
