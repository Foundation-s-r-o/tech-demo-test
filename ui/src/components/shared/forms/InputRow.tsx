import React from 'react'

export default function InputRow({
    children,
}: {
    children: React.ReactElement[]
}) {
    return (
        <div className="row py-1">
            {children.map((child, index) => {
                return (
                    <div
                        key={index}
                        className={`col-lg-6 ${
                            index === 0 ? 'text-lg-end' : ''
                        }`}>
                        {child}
                    </div>
                )
            })}
        </div>
    )
}
