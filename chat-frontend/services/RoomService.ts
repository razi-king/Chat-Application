import api, { ApiEndpoints, request } from "./api";
import type { Message, PageResponse, Room } from "@/types";

export class RoomService {
  static myConversations() {
    return request<Room[]>(api.get(ApiEndpoints.Rooms));
  }

  static getRoom(roomId: string) {
    return request<Room>(api.get(`${ApiEndpoints.Rooms}/${roomId}`));
  }

  static openDirect(userId: string) {
    return request<Room>(api.post(`${ApiEndpoints.Rooms}/direct`, { userId }));
  }

  static createGroup(body: { name: string; description?: string; memberIds: string[] }) {
    return request<Room>(api.post(`${ApiEndpoints.Rooms}/group`, body));
  }

  static deleteRoom(roomId: string) {
    return request<void>(api.delete(`${ApiEndpoints.Rooms}/${roomId}`));
  }

  static addMembers(roomId: string, userIds: string[]) {
    return request<Room>(api.post(`${ApiEndpoints.Rooms}/${roomId}/members`, { userIds }));
  }

  static removeMember(roomId: string, userId: string) {
    return request<void>(api.delete(`${ApiEndpoints.Rooms}/${roomId}/members/${userId}`));
  }

  static markRead(roomId: string) {
    return request<void>(api.post(`${ApiEndpoints.Rooms}/${roomId}/read`));
  }

  static getMessages(roomId: string, before?: string, size = 30) {
    return request<PageResponse<Message>>(
      api.get(`${ApiEndpoints.Rooms}/${roomId}/messages`, { params: { before, size } })
    );
  }

  // clientMessageId Is The Idempotency Key: Retrying With The Same Id Never Creates A Duplicate
  static sendMessage(roomId: string, content: string, replyToId?: string, clientMessageId?: string) {
    return request<Message>(api.post(`${ApiEndpoints.Rooms}/${roomId}/messages`, { content, replyToId, clientMessageId }));
  }

  static pinned(roomId: string) {
    return request<Message[]>(api.get(`${ApiEndpoints.Rooms}/${roomId}/pinned`));
  }

  static search(roomId: string, q: string) {
    return request<Message[]>(api.get(`${ApiEndpoints.Rooms}/${roomId}/search`, { params: { q } }));
  }
}
