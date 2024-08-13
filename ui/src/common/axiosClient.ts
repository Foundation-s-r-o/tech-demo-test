import axios, { AxiosInstance, InternalAxiosRequestConfig  } from 'axios'
import _ from 'lodash'
import QueryString from 'qs'

export const API_SERVER_URL = process.env.APP_API_SERVER_URL

const axiosClient: AxiosInstance = axios.create({
    baseURL: API_SERVER_URL,
})

axiosClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    config.headers['Cache-Control'] = 'no-cache'
    config.headers['Pragma'] = 'no-cache'
    config.headers['Expires'] = '0'
    /*eslint-disable */
    config.paramsSerializer = (params: any) => {
        if (params) {
            // Remove values: null, undefined, ''
            params = params
                ? _.reduce(
                    params,
                    (acc: Record<string, any>, value: any, key: string) => {
                        if (
                            value !== null &&
                            value !== undefined &&
                            value !== ''
                        ) {
                            acc[key] = value
                        }
                        return acc
                    },
                    {}
                )
                : {}

            // transform Date properties to format YYYY-MM-DD
            params = _.mapValues(params, (value: any) => {
                if (value instanceof Date) {
                    return value.toISOString().split('T')[0]
                }
                return value
            })

            return QueryString.stringify(params, { arrayFormat: 'repeat' })
        }
    }
    /*eslint-enable */
    return config
})

export default axiosClient
