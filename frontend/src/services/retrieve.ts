import { fileToBase64, stripBase64Prefix } from "@/lib/utils";
import { searchByText, uploadImageBase64 } from "@/api";

type RetrieveParams = { file: File } | { textQuery: string };

export async function retrieveService(params: RetrieveParams) {
  if ("file" in params) {
    const base64 = await fileToBase64(params.file);

    return uploadImageBase64({
      filename: params.file.name,
      imageBase64: stripBase64Prefix(base64),
    });
  }

  if ("textQuery" in params) {
    return searchByText(params.textQuery);
  }

  throw new Error("Invalid params");
}
