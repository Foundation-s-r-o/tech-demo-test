import React, { PropsWithChildren } from 'react'

const FndtTextArea = (
    props: PropsWithChildren & {
        value?: string
        id?: string
        name?: string
    }
) => {
    return (
        <div className="form-value">
            <textarea
                {...props}
                className="form-control"
            />
        </div>
    )
}

export default FndtTextArea
