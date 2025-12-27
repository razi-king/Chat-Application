import React from 'react'
import ReusableForm from '../form/ReusableForm'
import { FormStyles } from '../enums/FormStyles'
import { joinRoomSchema } from '../form/formSchema'

const JoinRoomChat = () => {
    const initialValues = {name: '', roomId: ''}
    const field = [
        {name: 'name', label: 'Name', placeHolder: 'Enter Your Name'},
        {name: 'roomId', label: 'Room Id / New Room Id', placeHolder: 'Enter New Room Id'},
    ]
  return (
    <ReusableForm
      initialValues={initialValues}
      onSubmit={} 
      validationSchema={joinRoomSchema}
      fields={field}
      formTitle='Join Room'
      buttonText='Join'
      formStyle={FormStyles.JOINROOMFORM}
    />
  )
}

export default JoinRoomChat