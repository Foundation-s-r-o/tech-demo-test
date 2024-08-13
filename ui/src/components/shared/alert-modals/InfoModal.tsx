import { isFunction } from 'lodash'
import React, { useState } from 'react'
import { Modal } from 'react-bootstrap'
import { useTranslation } from 'react-i18next'
import FndtModal from '../../modals/Modal'
import { FndtButton } from '../buttons/FndtButton'

type InfoModalProps = {
    onClose?: () => void
    content?: React.ReactNode
}

export default function InfoModal({ content, onClose = null }: InfoModalProps) {
    const { t } = useTranslation()
    const [show, setShow] = useState(true)

    const handleClose = () => {
        if (isFunction(onClose)) {
            onClose()
        }
        setShow(false)
    }

    return (
        <FndtModal
            show={show}
            backdrop="static"
            centered
            dialogClassName="alert-modal info-modal"
            onHide={handleClose}>
            <Modal.Header>
                <Modal.Title>{t('component.title.informacneOkno')}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <div id="info_dialog_msg">{content}</div>
            </Modal.Body>
            <Modal.Footer>
                <FndtButton
                    variant="secondary"
                    onClick={handleClose}>
                    {t('common.btn.close')}
                </FndtButton>
            </Modal.Footer>
        </FndtModal>
    )
}
