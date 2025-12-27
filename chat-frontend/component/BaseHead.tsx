import React from 'react'
interface Props {
    title?: string;
    description?: string;
}
const BaseHead = ({title = "Razi Chat App",description="This App Is Created By The Greatest Full Stack Developer Named Razi"}: Props) => {
  return (
    <head>
        <title>{title}</title>
        
    </head>
  )
}

export default BaseHead