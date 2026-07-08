import { Role } from '@common/auth/Role'
import { HeroIcon } from '../shared/icons/FndtIcon'

export type MenuItemType = {
    name: string
    link: string
    icon?: HeroIcon
    // When set, the item is shown only to users holding one of these roles.
    roles?: Role[]
}

export type MenuGroupType = {
    key: string
    name: string
    icon?: HeroIcon
    roles?: Role[]
    children: MenuItemType[]
}

export type MenuTreeType = {
    items: MenuGroupType[]
}
