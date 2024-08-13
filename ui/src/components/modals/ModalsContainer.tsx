import React from 'react'
import { useModals } from './useModals'

const ModalsContainer = () => {
    const { openedModals, containerRef } = useModals()

    return (
        <div
            ref={containerRef}
            className={
                'modals-container contains-modals-' +
                Object.keys(openedModals).length
            }>
            {Object.keys(openedModals).map((modalKey) => {
                return openedModals[modalKey]
            })}
        </div>
    )
}

export default ModalsContainer
