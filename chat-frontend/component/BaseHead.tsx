import type { Metadata } from "next";

// In The App Router The <head> Is Built From "metadata" Exports, Not From A <head> Component
interface Props {
  title?: string;
  description?: string;
}

const BaseHead = ({
  title = "Nexus — Chat Without Limits",
  description = "Real-time chat that mixes WhatsApp style DMs and groups with Discord style servers and channels. Built by Razi.",
}: Props = {}): Metadata => ({
  title: { default: title, template: "%s · Nexus" },
  description,
  applicationName: "Nexus",
});

export default BaseHead;
