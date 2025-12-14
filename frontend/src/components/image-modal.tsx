import { createPortal } from "react-dom";

type ImageModalProps = {
  src: string;
  onClose: () => void;
};

export default function ImageModal({ src, onClose }: ImageModalProps) {
  if (typeof window === "undefined") return null;

  return createPortal(
    <div
      className="fixed inset-0 z-1000 flex items-center justify-center bg-black/70"
      onClick={onClose}
    >
      <img
        src={src}
        className="max-h-[90vh] max-w-[90vw] rounded-lg"
        onClick={(e) => e.stopPropagation()}
      />
    </div>,
    document.body,
  );
}
