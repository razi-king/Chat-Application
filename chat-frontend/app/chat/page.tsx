import { redirect } from 'next/navigation'

// Old Route: The Chat Now Lives Inside The App Shell
export default function page() {
  redirect('/app/chats')
}
