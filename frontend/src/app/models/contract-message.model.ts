export interface ContractMessage {
  id: number;
  contractId: number;
  senderId: number;
  content: string;
  createdAt?: string;
  contractProgressPercent?: number | null;
}
