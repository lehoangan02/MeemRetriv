import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

import { queryClient } from "@/lib/query-client";
import AppHeader from "@/components/app-header";
import MainContent from "@/components/main-content";

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="relative flex min-h-dvh w-full flex-col">
        <AppHeader className="sticky top-0 left-0 w-full bg-base-300" />
        <MainContent className="scrollbar flex-1 basis-0 overflow-y-auto scrollbar-stable-both scrollbar-thumb-base-content/30 scrollbar-track-base-200" />
      </div>
      <ReactQueryDevtools initialIsOpen={false} client={queryClient} />
    </QueryClientProvider>
  );
}

export default App;
