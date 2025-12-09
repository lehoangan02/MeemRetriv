import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import AppHeader from "@/components/app-header";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <div className="relative min-h-dvh w-full">
        <AppHeader className="sticky top-0 left-0 w-full bg-base-300" />
        <main className="h-1000 overflow-y-auto scrollbar-stable-both">
          Hello
        </main>
      </div>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

export default App;
