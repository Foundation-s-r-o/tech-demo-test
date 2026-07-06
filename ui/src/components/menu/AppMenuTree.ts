import { UsersIcon } from '@heroicons/react/24/outline'
import routes from '../router/routes'
import i18n from '@common/i18n'
import { MenuTreeType } from './types'

const menuTree: MenuTreeType = {
    items: [
        {
            key: '1',
            name: i18n.t('component.persons.title'),
            icon: UsersIcon,
            children: [
                {
                    name: i18n.t('component.persons.list'),
                    link: routes.personsList.path,
                },
                {
                    name: i18n.t('component.persons.add'),
                    link: routes.personsAdd.path,
                }
            ],
        },
    ],
}

export default menuTree
