import routes from '../router/routes'
import i18n from '@common/i18n'

const menuTree = {
    items: [
        {
            key: '1',
            name: i18n.t('component.persons.title'),
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
