import React from 'react'

const PrettyPrintJson = ({ data }: { data: unknown }) => (
    <div>
        <pre>{JSON.stringify(data, null, 2)}</pre>
    </div>
)

export default PrettyPrintJson
