import {
    AuthControllerApi,
    Configuration,
    PersonControllerApi
} from './generated'
import { BaseAPI } from './generated/base'
import axiosClient from '@common/axiosClient'
import { AxiosInstance } from 'axios'

interface Constructable<T> {
    new (configuration?: Configuration, basePath?: string, axios?: AxiosInstance): T
}

const config = new Configuration()

function createApiInstance<T extends BaseAPI>(ApiClass: Constructable<T>): T {
    return new ApiClass(config, '', axiosClient)
}

export const authControllerApi =
    createApiInstance(AuthControllerApi)

export const personControllerApi =
    createApiInstance(PersonControllerApi)

