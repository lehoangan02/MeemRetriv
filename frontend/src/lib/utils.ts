import clsx, { type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => {
      if (!reader.result) return reject("Empty file result");
      resolve(reader.result.toString());
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export function stripBase64Prefix(base64: string): string {
  if (!base64) return base64;

  const commaIndex = base64.indexOf(",");
  return commaIndex !== -1 ? base64.substring(commaIndex + 1) : base64;
}
