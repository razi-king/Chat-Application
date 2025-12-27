import * as Yup from 'yup'

export const joinRoomSchema = Yup.object({
    name: Yup.string()
        .min(2, "Name Must Have Atleast Two Letters")
        .max(50, "Name Must Not Exceed 50 Charecters")
        .required("Name Cannot Be Empty"),
    roomId: Yup.string()
        .required("Please Enter New Room Id")
})