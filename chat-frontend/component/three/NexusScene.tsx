"use client";
import React, { useMemo, useRef } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Float, MeshDistortMaterial, Sparkles, Stars, RoundedBox } from "@react-three/drei";
import * as THREE from "three";

export type SceneVariant = "hero" | "auth";

// ---------- The Glowing Core: Distorted Crystal + Wireframe Shell ----------
function Core({ variant }: { variant: SceneVariant }) {
  const shell = useRef<THREE.Mesh>(null);
  useFrame((_, dt) => {
    if (shell.current) {
      shell.current.rotation.y += dt * 0.18;
      shell.current.rotation.x += dt * 0.07;
    }
  });
  const scale = variant === "hero" ? 1.35 : 1.05;
  return (
    <Float speed={1.6} rotationIntensity={0.6} floatIntensity={1.1}>
      <mesh scale={scale}>
        <icosahedronGeometry args={[1, 24]} />
        <MeshDistortMaterial
          color="#0e7490"
          emissive="#22d3ee"
          emissiveIntensity={0.55}
          roughness={0.15}
          metalness={0.7}
          distort={0.38}
          speed={1.8}
        />
      </mesh>
      <mesh ref={shell} scale={scale * 1.32}>
        <icosahedronGeometry args={[1, 1]} />
        <meshBasicMaterial color="#a78bfa" wireframe transparent opacity={0.35} />
      </mesh>
    </Float>
  );
}

// ---------- Orbit Rings ----------
function Ring({ radius, tilt, color, speed }: { radius: number; tilt: [number, number, number]; color: string; speed: number }) {
  const ref = useRef<THREE.Mesh>(null);
  useFrame((_, dt) => {
    if (ref.current) ref.current.rotation.z += dt * speed;
  });
  return (
    <mesh ref={ref} rotation={tilt}>
      <torusGeometry args={[radius, 0.012, 16, 160]} />
      <meshBasicMaterial color={color} transparent opacity={0.75} toneMapped={false} />
    </mesh>
  );
}

// ---------- Chat Bubbles Orbiting The Core (Messages Flying Around The Network) ----------
function Bubbles({ count = 7 }: { count?: number }) {
  const group = useRef<THREE.Group>(null);
  const items = useMemo(
    () =>
      Array.from({ length: count }, (_, i) => ({
        angle: (i / count) * Math.PI * 2,
        radius: 2.4 + (i % 3) * 0.35,
        y: Math.sin(i * 1.7) * 0.9,
        color: ["#22d3ee", "#a78bfa", "#f472b6", "#34d399"][i % 4],
        w: 0.42 + (i % 2) * 0.18,
      })),
    [count]
  );
  useFrame((state) => {
    if (!group.current) return;
    const t = state.clock.elapsedTime;
    group.current.children.forEach((child, i) => {
      const it = items[i];
      const a = it.angle + t * 0.25;
      child.position.set(Math.cos(a) * it.radius, it.y + Math.sin(t + i) * 0.15, Math.sin(a) * it.radius);
      child.lookAt(0, child.position.y, 0);
    });
  });
  return (
    <group ref={group}>
      {items.map((it, i) => (
        <RoundedBox key={i} args={[it.w, 0.26, 0.06]} radius={0.1} smoothness={4}>
          <meshStandardMaterial color={it.color} emissive={it.color} emissiveIntensity={0.9} transparent opacity={0.85} />
        </RoundedBox>
      ))}
    </group>
  );
}

// ---------- Mouse Parallax For The Whole Scene ----------
function Rig({ children }: { children: React.ReactNode }) {
  const ref = useRef<THREE.Group>(null);
  useFrame((state) => {
    if (!ref.current) return;
    ref.current.rotation.y = THREE.MathUtils.lerp(ref.current.rotation.y, state.pointer.x * 0.35, 0.05);
    ref.current.rotation.x = THREE.MathUtils.lerp(ref.current.rotation.x, -state.pointer.y * 0.2, 0.05);
  });
  return <group ref={ref}>{children}</group>;
}

export default function NexusScene({ variant = "hero" }: { variant?: SceneVariant }) {
  // Phones: Fewer Particles + Lower Pixel Ratio -> Smooth Frame Rate And Less Battery / Memory
  const small = typeof window !== "undefined" && window.innerWidth < 768;
  const hero = variant === "hero";
  return (
    <Canvas
      dpr={small ? [1, 1.25] : [1, 1.75]}
      camera={{ position: [0, 0, variant === "hero" ? 6.2 : 7], fov: 45 }}
      gl={{ antialias: true, alpha: true, powerPreference: "high-performance" }}
    >
      <ambientLight intensity={0.35} />
      <pointLight position={[4, 3, 4]} intensity={40} color="#22d3ee" />
      <pointLight position={[-4, -2, 2]} intensity={30} color="#f472b6" />
      <pointLight position={[0, 4, -4]} intensity={25} color="#a78bfa" />
      <Rig>
        <Core variant={variant} />
        <Ring radius={2.05} tilt={[Math.PI / 2.4, 0, 0]} color="#22d3ee" speed={0.35} />
        <Ring radius={2.45} tilt={[Math.PI / 1.8, 0.4, 0]} color="#a78bfa" speed={-0.25} />
        {variant === "hero" && <Ring radius={2.9} tilt={[Math.PI / 2.1, -0.5, 0.3]} color="#f472b6" speed={0.18} />}
        <Bubbles count={small ? 4 : hero ? 8 : 5} />
      </Rig>
      <Sparkles count={small ? 30 : hero ? 90 : 50} scale={[9, 6, 6]} size={2.2} speed={0.35} color="#a5f3fc" />
      <Stars radius={60} depth={40} count={small ? 700 : hero ? 2500 : 1200} factor={3} saturation={0} fade speed={0.6} />
    </Canvas>
  );
}
