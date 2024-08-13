import {
    AuthControllerApi,
    Configuration,
    PersonControllerApi
} from './generated'
import { BaseAPI } from './generated/base'
import axiosClient from '@common/axiosClient'

interface Constructable<T> {
    new (...args: unknown[]): T
}

const config = new Configuration()

function createApiInstance<T extends BaseAPI>(ApiClass: Constructable<T>): T {
    return new ApiClass(config, '', axiosClient)
}

export const authControllerApi =
    createApiInstance(AuthControllerApi)

export const personControllerApi =
    createApiInstance(PersonControllerApi)

