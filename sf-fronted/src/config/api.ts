//请求地址，若未指定，则默认用本地8080端口，开发环境在.env.development中，生产环境在.env.production中
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
