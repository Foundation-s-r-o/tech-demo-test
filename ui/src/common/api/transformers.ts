import { AxiosPromise, AxiosResponse } from 'axios'
import _, { isFunction } from 'lodash'

export const transformResponse = <Response, Entity>(
    axiosPromise: AxiosPromise<Response>,
    transformer: (response: Response) => Entity
): AxiosPromise<Entity> => {
    return axiosPromise.then((response: AxiosResponse<Response>) => {
        return {
            ...response,
            data: transformer(response.data),
        }
    })
}

export const transformArrayOfT1ToArrayOfT2 = <T1, T2>(
    arrayToTransform: T1[],
    customTransformFn: (
        entityToTransform: T1
    ) => T2 = null
): T2[] => {
    const output: T2[] = []
    arrayToTransform.forEach(i => {
        output.push((isFunction(customTransformFn)
            ? customTransformFn({ ...i })
            : { ...i }) as unknown as T2)
    })
    return output
}
/*eslint-disable */
export const transformRequest = <Request extends Record<string, unknown>>(
    entityToTransform: Request,
    customTransformFn: ((entityToTransform: Request) => Request) | null = null
): Request => {
    const requestEntity: Request = ((entity: Request) => {
        let processedEntity: Request
        processedEntity = isFunction(customTransformFn) ? customTransformFn({ ...entity }) : { ...entity }
        // Remove values: null, undefined, ''
        processedEntity = _.reduce(
            processedEntity,
            (acc: any, value: unknown, key: string) => { // Cast acc to any temporarily
                if (value !== null && value !== undefined && value !== '') {
                    acc[key] = value
                }
                return acc
            },
            {}
        );
        // transform Date properties to format YYYY-MM-DD
        processedEntity = _.mapValues(processedEntity, (value: unknown) => {
            if (value instanceof Date) {
                return value.toISOString().split('T')[0]
            }
            return value
        }) as Request

        return processedEntity
    })(entityToTransform)
    return requestEntity as Request
}
/*eslint-enable */
