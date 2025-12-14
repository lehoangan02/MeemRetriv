import React, { type ChangeEventHandler } from "react";
import {
  LinkIcon,
  FilePlusIcon,
  FolderOpenIcon,
  MenuIcon,
  HistoryIcon,
} from "lucide-react";

interface InputFileButtonProps {
  inputRef: React.RefObject<HTMLInputElement | null>;
  onFileSelected: (file: File) => void;
}

export default function InputFileButton({
  inputRef,
  onFileSelected,
}: InputFileButtonProps) {
  const handleFileChange: ChangeEventHandler<HTMLInputElement> = (e) => {
    const file = e.target.files?.[0];
    if (file) onFileSelected(file);
    e.target.value = "";
  };

  return (
    <div className="join">
      <button
        className="btn join-item h-fit px-4 py-3 btn-primary"
        onClick={() => inputRef.current?.click()}
      >
        <FilePlusIcon />
        <span className="text-lg">Choose Files</span>
      </button>

      <input
        ref={inputRef}
        type="file"
        className="hidden"
        accept="image/*"
        onChange={handleFileChange}
      />

      <div className="dropdown dropdown-right dropdown-end">
        <div
          tabIndex={0}
          role="button"
          className="btn join-item h-full btn-primary"
        >
          <MenuIcon />
        </div>
        <ul
          tabIndex={-1}
          className="dropdown-content menu z-1 ml-1 min-w-56 rounded-box bg-primary px-1 py-2 text-primary-content shadow-xl"
        >
          <li>
            <button
              className="inline-flex items-center gap-2 px-4 py-3 leading-none"
              onClick={() => inputRef.current?.click()}
            >
              <FolderOpenIcon className="size-4" />
              From Computer
            </button>
          </li>
          <li>
            <button className="inline-flex items-center gap-2 px-4 py-3 leading-none">
              <LinkIcon className="size-4" />
              By URL
            </button>
          </li>
          <li>
            <button className="inline-flex items-center gap-2 px-4 py-3 leading-none">
              <HistoryIcon className="size-4" />
              HIstory
            </button>
          </li>
        </ul>
      </div>
    </div>
  );
}
