export interface Message {
  id: number;
  contractId: number;
  senderId: number;
  content: string;
  createdAt: string;
}

export interface MessageRequest {
  senderId: number;
  content: string;
}
