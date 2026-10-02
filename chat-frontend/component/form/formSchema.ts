import * as Yup from 'yup'

// Same Rules As The Backend DTO Annotations, So Most Errors Are Caught Before The Request

export const loginSchema = Yup.object({
    identifier: Yup.string().trim().required("Enter your username or email"),
    password: Yup.string().required("Enter your password"),
})

export const registerSchema = Yup.object({
    displayName: Yup.string().trim()
        .max(40, "Display name must not exceed 40 characters")
        .required("Display name is required"),
    username: Yup.string().trim()
        .min(3, "Username must be 3-20 characters")
        .max(20, "Username must be 3-20 characters")
        .matches(/^[a-zA-Z0-9_.]+$/, "Only letters, numbers, dot and underscore")
        .required("Username is required"),
    email: Yup.string().trim().email("Enter a valid email").required("Email is required"),
    password: Yup.string()
        .min(6, "Password must be at least 6 characters")
        .max(64, "Password is too long")
        .required("Password is required"),
    confirmPassword: Yup.string()
        .oneOf([Yup.ref('password')], "Passwords do not match")
        .required("Confirm your password"),
})

// Join A Server With An Invite Code (Old "Join Room" Form)
export const joinRoomSchema = Yup.object({
    roomId: Yup.string().trim()
        .min(4, "Invite code is too short")
        .required("Please enter an invite code or link"),
})

export const createServerSchema = Yup.object({
    name: Yup.string().trim()
        .min(2, "Name must have at least 2 letters")
        .max(40, "Name must not exceed 40 characters")
        .required("Server name is required"),
    description: Yup.string().max(200, "Description must not exceed 200 characters"),
})

export const createGroupSchema = Yup.object({
    name: Yup.string().trim()
        .min(2, "Group name must have at least 2 letters")
        .max(40, "Group name must not exceed 40 characters")
        .required("Group name is required"),
    description: Yup.string().max(120, "Description must not exceed 120 characters"),
})

export const createChannelSchema = Yup.object({
    name: Yup.string().trim()
        .max(30, "Channel name must not exceed 30 characters")
        .matches(/^[a-z0-9-]+$/, "Use lowercase letters, numbers and dashes")
        .required("Channel name is required"),
    description: Yup.string().max(120, "Topic must not exceed 120 characters"),
})

export const addFriendSchema = Yup.object({
    username: Yup.string().trim().min(3, "Username is too short").required("Enter a username"),
})

export const profileSchema = Yup.object({
    displayName: Yup.string().trim().min(1).max(40, "Display name must not exceed 40 characters").required("Display name is required"),
    about: Yup.string().max(140, "About must not exceed 140 characters"),
    customStatus: Yup.string().max(60, "Status must not exceed 60 characters"),
})
