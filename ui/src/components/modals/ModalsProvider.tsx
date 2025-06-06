import React, { PropsWithChildren, useRef, useState } from 'react'
import { uniqueId } from 'lodash'
import { ModalsContextProvider } from './ModalsContext'
import { ModalsStackSettings } from './types'
import ModalsContainer from './ModalsContainer'

export default function ModalsProvider({ children }: PropsWithChildren) {
    const [openedModals, setOpenedModals] = useState<{
        [modalKey: string]: React.ReactElement<PropsWithChildren>
    }>({})
    const containerRef = useRef<HTMLDivElement | null>(null)

    const showModal = (
        modal: React.ReactElement<PropsWithChildren>,
        settings: ModalsStackSettings = {}
    ) => {
        const modalKey: string = uniqueId('modal')
        const newModal = modal
            ? (React.cloneElement(modal, {
                ...modal.props,
                key: modalKey,
            }) as unknown as React.ReactElement)
            : null

        if (newModal) {
            setOpenedModals((prev) => {
                return {
                    ...(settings?.keepParentModalsOpened ? { ...prev } : null),
                    [modalKey]: newModal,
                }
            })

            return modalKey
        }

        return null
    }

    const hideModal = (modalKey: string) => {
        setOpenedModals((prev) => {
            if (!modalKey) {
                return {}
            }

            const newModalStack = {
                ...prev,
            }
            delete newModalStack[modalKey]
            return newModalStack
        })
    }

    const api = {
        showModal,
        hideModal,
        openedModals,
        containerRef,
    }

    return (
        <ModalsContextProvider value={api}>
            <ModalsContainer />
            {children}
        </ModalsContextProvider>
    )
}
