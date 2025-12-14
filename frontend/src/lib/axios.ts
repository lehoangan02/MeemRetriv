import { ENV } from "@/config/env";
import axios from "axios";

const axiosClient = axios.create({
  baseURL: ENV.VITE_BACKEND_URL,
  //timeoutErrorMessage: "Requests take too long to respond",
});

axiosClient.interceptors.request.use(
  (config) => {
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

axios.interceptors.response.use(
  (res) => {
    return res;
  },
  (error) => {
    return Promise.reject(error);
  },
);

export default axiosClient;
