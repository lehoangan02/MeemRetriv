import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

import { queryClient } from "@/lib/query-client";
import AppHeader from "@/components/app-header";
import ImageUploader from "@/components/image-uploader";

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="relative flex min-h-dvh w-full flex-col">
        <AppHeader className="sticky top-0 left-0 w-full bg-base-300" />
        <main className="flex-1 basis-1 overflow-y-auto scrollbar-stable-both">
          <div className="h-full w-full">
            <ImageUploader />
          </div>
        </main>
      </div>
      <ReactQueryDevtools initialIsOpen={false} client={queryClient} />
    </QueryClientProvider>
  );
}

export default App;
