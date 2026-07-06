import { HeroIcon } from '../shared/icons/FndtIcon'

export type MenuItemType = {
    name: string
    link: string
    icon?: HeroIcon
}

export type MenuGroupType = {
    key: string
    name: string
    icon?: HeroIcon
    children: MenuItemType[]
}

export type MenuTreeType = {
    items: MenuGroupType[]
}
