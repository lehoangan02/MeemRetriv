import { create } from "zustand";

type ImageHistoryState = {
  images: Map<string, File>;
  append: (file: File) => void;
  clear: () => void;
};

function makeId(file: File) {
  return `${file.name}-${file.size}-${file.type}-${file.lastModified}`;
}

export const useImageHistory = create<ImageHistoryState>()((set) => {
  return {
    images: new Map(),
    append(file) {
      const id = makeId(file);

      set((state) => {
        if (state.images.has(id)) return state;
        const next = new Map(state.images);
        next.set(id, file);

        return { images: next };
      });
    },
    clear: () => set({ images: new Map() }),
  };
});
