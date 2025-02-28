import React from 'react'
import type { Meta, StoryObj } from '@storybook/react'
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
} as Meta<typeof FndtButton>

type Story = StoryObj<typeof FndtButton>

export const Button: Story = {
    render: (args: React.ComponentProps<typeof FndtButton>) => <FndtButton {...args}>{args.children}</FndtButton>,
    args: {
        size: FndtButtonSize.lg,
    }
}
