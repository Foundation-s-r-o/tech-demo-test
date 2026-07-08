import {
    ClipboardDocumentListIcon,
    DocumentCheckIcon,
    IdentificationIcon,
    QueueListIcon,
    ShieldCheckIcon,
    UserCircleIcon,
    UserPlusIcon,
    UsersIcon
} from '@heroicons/react/24/outline'
import routes from '../router/routes'
import i18n from '@common/i18n'
import { Role } from '@common/auth/Role'
import { MenuTreeType } from './types'

const REGISTRY_STAFF = [Role.CLERK, Role.SUPERVISOR, Role.DPO_OFFICER]
const CLERKS = [Role.CLERK, Role.SUPERVISOR]
const AUDITORS = [Role.DPO_OFFICER, Role.SECURITY_AUDITOR]

const menuTree: MenuTreeType = {
    items: [
        {
            key: 'persons',
            name: i18n.t('component.persons.title'),
            icon: UsersIcon,
            roles: REGISTRY_STAFF,
            children: [
                { name: i18n.t('component.persons.list'), link: routes.personsList.path, icon: QueueListIcon, roles: REGISTRY_STAFF },
                { name: i18n.t('component.persons.add'), link: routes.personsAdd.path, icon: UserPlusIcon, roles: CLERKS },
            ],
        },
        {
            key: 'records',
            name: i18n.t('component.records.title'),
            icon: ClipboardDocumentListIcon,
            roles: CLERKS,
            children: [
                { name: i18n.t('component.records.birth'), link: routes.recordsBirth.path, roles: CLERKS },
                { name: i18n.t('component.records.marriage'), link: routes.recordsMarriage.path, roles: CLERKS },
                { name: i18n.t('component.records.death'), link: routes.recordsDeath.path, roles: CLERKS },
            ],
        },
        {
            key: 'gdpr',
            name: i18n.t('component.gdpr.title'),
            icon: DocumentCheckIcon,
            roles: [Role.DPO_OFFICER],
            children: [
                { name: i18n.t('component.gdpr.title'), link: routes.gdpr.path, icon: DocumentCheckIcon, roles: [Role.DPO_OFFICER] },
            ],
        },
        {
            key: 'audit',
            name: i18n.t('component.audit.title'),
            icon: ShieldCheckIcon,
            roles: AUDITORS,
            children: [
                { name: i18n.t('component.audit.title'), link: routes.audit.path, icon: ShieldCheckIcon, roles: AUDITORS },
            ],
        },
        {
            key: 'me',
            name: i18n.t('component.me.title'),
            icon: UserCircleIcon,
            roles: [Role.CITIZEN],
            children: [
                { name: i18n.t('component.me.title'), link: routes.me.path, icon: IdentificationIcon, roles: [Role.CITIZEN] },
            ],
        },
    ],
}

export default menuTree
