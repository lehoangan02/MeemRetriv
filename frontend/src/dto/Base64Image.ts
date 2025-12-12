interface Base64Image {
  filename: string;
  imageBase64: string;
}

export interface Base64ImageRequest extends Base64Image {}

export interface Base64ImageResponse extends Base64Image {
  sentiment: string;
}
