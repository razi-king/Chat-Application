import api, { ApiEndpoints, request } from "./api";
import type { Message } from "@/types";

export class MessageService {
  static edit(messageId: string, content: string) {
    return request<Message>(api.put(`${ApiEndpoints.Messages}/${messageId}`, { content }));
  }

  static remove(messageId: string) {
    return request<Message>(api.delete(`${ApiEndpoints.Messages}/${messageId}`));
  }

  static react(messageId: string, emoji: string) {
    return request<Message>(api.post(`${ApiEndpoints.Messages}/${messageId}/reactions`, { emoji }));
  }

  static togglePin(messageId: string) {
    return request<Message>(api.post(`${ApiEndpoints.Messages}/${messageId}/pin`));
  }
}
