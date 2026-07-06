import React from 'react'

// One form field: label ABOVE the input (Filament-style). Occupies half the
// row on lg+ screens so section fields flow in a 2-column grid.
export default function InputRow({
    children,
}: {
    children: React.ReactElement[]
}) {
    return (
        <div className="col-lg-6 py-1 fndt_field">
            {children.map((child, index) => {
                return <div key={index}>{child}</div>
            })}
        </div>
    )
}
