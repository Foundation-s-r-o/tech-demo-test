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
                        className={index === 0 ? 'col-lg-2' : 'col-lg-6'}>
                        {child}
                    </div>
                )
            })}
        </div>
    )
}
