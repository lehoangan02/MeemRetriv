import { retrieveService } from "@/services/retrieve";
import { useMutation } from "@tanstack/react-query";

export default function useRetrieve() {
  const mutattion = useMutation({
    mutationFn: retrieveService,
  });

  return mutattion;
}
