import classNames from 'classnames'
import React from 'react'
import './Forms.scss'

// Form section card (Filament-style): optional heading + fields flowing in a
// 2-column grid (each InputRow is a col-lg-6).
export default function InputSection({
    className,
    title,
    children,
}: {
    children: React.ReactNode
    className?: string
    title?: string
}) {
    return (
        <section className={classNames('inputs-section', className)}>
            {title && <h3 className="inputs-section-title">{title}</h3>}
            <div className="row">{children}</div>
        </section>
    )
}
