import axios from 'axios'
import { API_BASE_URL } from '@/config/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token') || localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.token = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

export default request
