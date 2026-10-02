import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono, Space_Grotesk } from "next/font/google";
import "./globals.css";
import { Toaster } from "react-hot-toast";
import Navbar from "@/component/ui/Navbar";
import BaseHead from "@/component/BaseHead";
import { AuthProvider } from "@/context/AuthContext";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const display = Space_Grotesk({
  variable: "--font-display-face",
  subsets: ["latin"],
});

export const metadata: Metadata = BaseHead();

export const viewport: Viewport = {
  themeColor: "#04050c",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} ${display.variable} antialiased dark`}
      >
        <AuthProvider>
          <Navbar/>
          <Toaster
            position="top-right"
            toastOptions={{
              style: {
                background: "rgba(16,19,42,0.92)",
                color: "#e6e9ff",
                border: "1px solid rgba(148,163,255,0.18)",
                backdropFilter: "blur(12px)",
                borderRadius: "14px",
              },
              success: { iconTheme: { primary: "#34d399", secondary: "#04050c" } },
              error: { iconTheme: { primary: "#fb7185", secondary: "#04050c" } },
            }}
          />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
