import FndtInput from '@components/form/Input'
import FndtLabel from '@components/form/Label'
import FndtText from '@components/form/Text'
import InputRow from '@components/shared/forms/InputRow'
import { ComponentMeta, ComponentStory } from '@storybook/react'
import React from 'react'

export default {
    title: 'Common',
    component: InputRow,
} as ComponentMeta<typeof InputRow>

const Template: ComponentStory<typeof InputRow> = () => (
    <form>
        <div className="col-12">
            <div className="row">
                <InputRow>
                    <FndtLabel>FndtLabel</FndtLabel>
                    <FndtText>FndtText</FndtText>
                </InputRow>
                <InputRow>
                    <FndtLabel htmlFor="componentId_fieldName">
                        FndtLabel
                    </FndtLabel>
                    <FndtInput
                        id="componentId_fieldName"
                        name="ziadost.cisloZaznamu"
                        value="FndtInput"
                    />
                </InputRow>
            </div>
        </div>
    </form>
)

export const Form = Template.bind({})
