import React from 'react'
import { Modal } from 'react-bootstrap'
import {ModalProps} from 'react-bootstrap/Modal'
import { useModals } from './useModals'

const FndtModal = (props: ModalProps) => {
    const { containerRef } = useModals()

    return (
        <Modal
            container={containerRef}
            {...props}
        />
    )
}

export default FndtModal
