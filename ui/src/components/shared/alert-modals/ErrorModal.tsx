import { isFunction } from 'lodash'
import React, { useState } from 'react'
import { Modal } from 'react-bootstrap'
import { Trans, useTranslation } from 'react-i18next'
import FndtModal from '../../modals/Modal'
import { FndtButton } from '../buttons/FndtButton'

type ErrorModalProps = {
    onClose?: () => void
    text?: string
}

export default function ErrorModal({
    text = 'common.error.generic',
    onClose = null,
}: ErrorModalProps) {
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
            dialogClassName="alert-modal error-modal"
            onHide={handleClose}>
            <Modal.Header>
                <Modal.Title>{t('component.title.oznamOChybe')}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <div id="error_dialog_msg">
                    <Trans i18nKey={text} />
                </div>
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
