import React from 'react'
import './Forms.scss'

export default function InputSection({
    className,
    children,
}: {
    children: React.ReactNode
    className?: string
}) {
    return (
        <div className={`inputs-section${className ? ' ' + className : ''}`}>
            {children}
        </div>
    )
}
