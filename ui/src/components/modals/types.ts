import {PropsWithChildren} from 'react'

export interface ModalsContextType {
    showModal: (
        modal: React.ReactElement<PropsWithChildren>,
        settings?: ModalsStackSettings
    ) => string | null
    hideModal: (modalKey: string) => void
    openedModals: {
        [modalKey: string]: React.ReactElement<PropsWithChildren>
    }
    containerRef: React.RefObject<HTMLDivElement>
}

export interface ModalsStackSettings {
    keepParentModalsOpened?: boolean
}
