const persistService: {
    remove: (key: string) => void
    set: (key: string, value: unknown) => void
    get: (key: string) => unknown
} = {
    get: (key) => {
        let value = localStorage.getItem(key)
        if (value) {
            try {
                value = JSON.parse(value)
            } catch {
                // todo
            }
        }
        return value
    },
    set: (key, value) => {
        localStorage.setItem(key, JSON.stringify(value))
    },
    remove: (key) => {
        localStorage.removeItem(key)
    },
}

export default persistService
