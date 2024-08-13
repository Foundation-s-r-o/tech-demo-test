import React from 'react'
import { ComponentStory, ComponentMeta } from '@storybook/react'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { FndtButtonSize } from '@components/shared/buttons/types'

export default {
    title: 'Common',
    component: FndtButton,
    args: {
        children: 'Button text',
        variant: 'primary',
        disabled: false,
    },
} as ComponentMeta<typeof FndtButton>

const Template: ComponentStory<typeof FndtButton> = (args) => (
    <FndtButton {...args}>{args.children}</FndtButton>
)

export const Button = Template.bind({})
Button.args = {
    size: FndtButtonSize.lg,
}
