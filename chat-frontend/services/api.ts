import axios, { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { log } from 'console';
const ApiBase: string = process.env.PUBLIC_API_BASE_URL as string;

export const ApiEndpoints = {
    Room: `${ApiBase}/room`,
    Message: `${ApiBase}/message`
} as const;

const api: AxiosInstance = axios.create({
    baseURL: ApiBase,
    headers: {
        'Content-Type': 'application/json'
    },
    timeout: 10000,
})

// Api Request Interceptors
api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        console.log(`Api Request ${config.method?.toUpperCase} ${config.url}`);
        return config;
    },
    (error: any) => {
        return Promise.reject(error);
    }
);

// Api Response Interceptors
api.interceptors.response.use(
    (response: AxiosResponse) => {
        return response;
    },
    async (error: AxiosError) => {
        const status = error.response ? error.response.status : null;
        if (status===401) {
            console.log('Un Authorized');
        }
        else if (status===404) {
            console.log('Method Not Found');
        }
        else if (status===500) {
            console.log('Un Expected Error Razi Se Pucho');
        }
        else {
            console.log('Razi Bhai Ye Error Mujhe Nahi Pata Kya Hai');
        }
    }
)

