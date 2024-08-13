import { PropsWithChildren } from 'react'

export type ListItemType = {
    name: string
    link: string
}

export type MenuRowPropsType = {
    child: {
        link?: string
        name: string
        listItem?: boolean
        items?: ListItemType[]
        listItemKey?: string
    }
    iter?: number
}
export type MenuCollapsePropsType = {
    eventKey: string
} & PropsWithChildren
