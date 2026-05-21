import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, timeout } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ChatbotService {

  private readonly url = 'http://localhost:8090/api/chatbot/chat';
  private readonly TIMEOUT_MS = 100_000;

  constructor(private http: HttpClient) {}

  chat(mensaje: string): Observable<string> {
    return this.http.post<{ respuesta: string }>(this.url, { mensaje }).pipe(
      timeout(this.TIMEOUT_MS),
      map(res => res.respuesta)
    );
  }
}
