import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Message, MessageRequest } from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly api = '/api/contracts';

  constructor(private http: HttpClient) {}

  getMessages(contractId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.api}/${contractId}/messages`);
  }

  sendMessage(contractId: number, request: MessageRequest): Observable<Message> {
    return this.http.post<Message>(`${this.api}/${contractId}/messages`, request);
  }
}
