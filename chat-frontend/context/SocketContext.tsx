"use client";
import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import { WsUrl, AUTH_EXPIRED_EVENT } from "@/services/api";
import { ApiError, ErrorCodes } from "@/lib/ApiError";
import { handleError } from "@/lib/errorHandler";
import type { ApiResponse, ChatEvent } from "@/types";

type Handler = (event: ChatEvent) => void;

interface SocketContextValue {
  connected: boolean;
  /** Subscribe to a STOMP destination; survives reconnects. Returns an unsubscribe function. */
  subscribe: (destination: string, handler: Handler) => () => void;
  /** Personal events (/user/queue/events) and presence (/topic/presence). */
  onEvent: (handler: Handler) => () => void;
  /** Throws ApiError(NETWORK_ERROR) when offline so callers can fall back to REST. */
  publish: (destination: string, body: unknown) => void;
}

const SocketContext = createContext<SocketContextValue | null>(null);

interface Registration {
  destination: string;
  handler: Handler;
  stomp?: StompSubscription;
}

export function SocketProvider({ token, children }: { token: string | null; children: React.ReactNode }) {
  const clientRef = useRef<Client | null>(null);
  const registry = useRef(new Map<number, Registration>());
  const listeners = useRef(new Set<Handler>());
  const nextId = useRef(1);
  const [connected, setConnected] = useState(false);

  const attach = useCallback((client: Client, reg: Registration) => {
    reg.stomp = client.subscribe(reg.destination, (msg: IMessage) => {
      try {
        reg.handler(JSON.parse(msg.body) as ChatEvent);
      } catch (e) {
        console.error("Bad socket payload", e);
      }
    });
  }, []);

  useEffect(() => {
    if (!token) return;
    const emit = (event: ChatEvent) => listeners.current.forEach((l) => l(event));
    const client = new Client({
      brokerURL: WsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {},
    });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe("/user/queue/events", (m) => emit(JSON.parse(m.body)));
      client.subscribe("/topic/presence", (m) => emit(JSON.parse(m.body)));
      // Backend @MessageExceptionHandler -> Same Response Envelope As REST -> Same Error Handler
      client.subscribe("/user/queue/errors", (m) => {
        const body = JSON.parse(m.body) as ApiResponse<unknown>;
        handleError(ApiError.fromResponse(body, 400));
      });
      registry.current.forEach((reg) => attach(client, reg));
    };

    // ERROR Frame From WebSocketAuthInterceptor, e.g. "AUTH_403: Session expired"
    client.onStompError = (frame) => {
      const text = frame.headers["message"] ?? "";
      if (text.startsWith("AUTH_")) {
        client.deactivate();
        window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
      } else if (text) {
        handleError(new ApiError(text.replace(/^\w+: /, ""), text.split(":")[0] || ErrorCodes.UNKNOWN));
      }
    };
    client.onWebSocketClose = () => setConnected(false);

    client.activate();
    clientRef.current = client;
    const regs = registry.current;
    return () => {
      regs.forEach((reg) => (reg.stomp = undefined));
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [token, attach]);

  const subscribe = useCallback(
    (destination: string, handler: Handler) => {
      const id = nextId.current++;
      const reg: Registration = { destination, handler };
      registry.current.set(id, reg);
      const client = clientRef.current;
      if (client?.connected) attach(client, reg);
      return () => {
        try {
          reg.stomp?.unsubscribe();
        } catch {
          // Already Closed
        }
        registry.current.delete(id);
      };
    },
    [attach]
  );

  const onEvent = useCallback((handler: Handler) => {
    listeners.current.add(handler);
    return () => {
      listeners.current.delete(handler);
    };
  }, []);

  const publish = useCallback((destination: string, body: unknown) => {
    const client = clientRef.current;
    if (!client?.connected) throw ApiError.network();
    client.publish({ destination, body: JSON.stringify(body) });
  }, []);

  const value = useMemo(() => ({ connected, subscribe, onEvent, publish }), [connected, subscribe, onEvent, publish]);
  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>;
}

export function useSocket(): SocketContextValue {
  const ctx = useContext(SocketContext);
  if (!ctx) throw new Error("useSocket must be used inside <SocketProvider>");
  return ctx;
}
