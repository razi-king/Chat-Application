import api, { ApiEndpoints, request } from "./api";
import type { AuditLog, Invite, MemberRole, PageResponse, Room, Server, ServerDetail, ServerMember } from "@/types";

export interface ServerPayload {
  name: string;
  description?: string;
  iconColor?: string;
  discoverable?: boolean;
}

export class ServerService {
  static mine() {
    return request<Server[]>(api.get(ApiEndpoints.Servers));
  }

  static discover() {
    return request<Server[]>(api.get(`${ApiEndpoints.Servers}/discover`));
  }

  static create(body: ServerPayload) {
    return request<Server>(api.post(ApiEndpoints.Servers, body));
  }

  static detail(serverId: string) {
    return request<ServerDetail>(api.get(`${ApiEndpoints.Servers}/${serverId}`));
  }

  static update(serverId: string, body: ServerPayload) {
    return request<Server>(api.put(`${ApiEndpoints.Servers}/${serverId}`, body));
  }

  static remove(serverId: string) {
    return request<void>(api.delete(`${ApiEndpoints.Servers}/${serverId}`));
  }

  static join(serverId: string) {
    return request<Server>(api.post(`${ApiEndpoints.Servers}/${serverId}/join`));
  }

  static leave(serverId: string) {
    return request<void>(api.post(`${ApiEndpoints.Servers}/${serverId}/leave`));
  }

  static kick(serverId: string, userId: string) {
    return request<void>(api.delete(`${ApiEndpoints.Servers}/${serverId}/members/${userId}`));
  }

  static updateRole(serverId: string, userId: string, role: MemberRole) {
    return request<ServerMember>(api.put(`${ApiEndpoints.Servers}/${serverId}/members/${userId}/role`, { role }));
  }

  static createChannel(serverId: string, body: { name: string; description?: string }) {
    return request<Room>(api.post(`${ApiEndpoints.Servers}/${serverId}/channels`, body));
  }

  static createInvite(serverId: string, body: { maxUses: number; expiresInHours: number }) {
    return request<Invite>(api.post(`${ApiEndpoints.Servers}/${serverId}/invites`, body));
  }

  static auditLogs(serverId: string, page = 0) {
    return request<PageResponse<AuditLog>>(api.get(`${ApiEndpoints.Servers}/${serverId}/audit-logs`, { params: { page } }));
  }
}

export class InviteService {
  static preview(code: string) {
    return request<Invite>(api.get(`${ApiEndpoints.Invites}/${code}`));
  }

  static accept(code: string) {
    return request<Server>(api.post(`${ApiEndpoints.Invites}/${code}/accept`));
  }
}
