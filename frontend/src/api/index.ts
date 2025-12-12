import axiosClient from "@/lib/axios";
import type {
  Base64ImageRequest,
  Base64ImageResponse,
} from "@/dto/Base64Image";

export async function uploadImageBase64(req: Base64ImageRequest) {
  try {
    const res = await axiosClient.post<Base64ImageResponse[]>(
      "/uploadImageBase64",
      req,
      {
        headers: { "Content-Type": "application/json" },
      },
    );
    return res.data;
  } catch (error) {
    throw error;
  }
}

export async function searchByText(textQuery: string) {
  try {
    const res = await axiosClient.post<Base64ImageResponse[]>(
      "/searchByText",
      textQuery,
      {
        headers: { "Content-Type": "text/plain" },
      },
    );
    return res.data;
  } catch (error) {
    throw error;
  }
}
