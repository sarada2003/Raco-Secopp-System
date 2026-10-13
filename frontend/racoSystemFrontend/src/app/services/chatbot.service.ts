import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Error con un mensaje ya pensado para el usuario (evento 'error' que envía el backend). */
export class ErrorAsistente extends Error {}

/**
 * Consume POST /api/chatbot/chat/stream (Server-Sent Events). Se usa fetch y no HttpClient
 * porque HttpClient entrega el cuerpo solo cuando la respuesta termina; EventSource tampoco
 * sirve porque solo hace GET y no permite enviar el header Authorization. Por eso este
 * servicio agrega el JWT a mano (el AuthInterceptor no intercepta fetch).
 */
@Injectable({ providedIn: 'root' })
export class ChatbotService {

  private readonly url = 'http://localhost:8090/api/chatbot/chat/stream';
  private readonly TIMEOUT_MS = 100_000;

  /** Emite cada fragmento de texto de la respuesta a medida que llega. */
  chatStream(mensaje: string): Observable<string> {
    return new Observable<string>(subscriber => {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), this.TIMEOUT_MS);
      const token = localStorage.getItem('token');

      const leer = async () => {
        const res = await fetch(this.url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
          },
          body: JSON.stringify({ mensaje }),
          signal: controller.signal
        });
        if (!res.ok || !res.body) throw new Error(`HTTP ${res.status}`);

        const reader = res.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';

        while (true) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          // Cada evento SSE termina en una línea vacía.
          let fin: number;
          while ((fin = buffer.indexOf('\n\n')) !== -1) {
            const evento = this.parsearEvento(buffer.slice(0, fin));
            buffer = buffer.slice(fin + 2);

            if (evento.tipo === 'fin') return;
            if (evento.tipo === 'error') throw new ErrorAsistente(evento.texto);
            if (evento.texto) subscriber.next(evento.texto);
          }
        }
      };

      leer()
        .then(() => subscriber.complete())
        .catch(err => subscriber.error(err))
        .finally(() => clearTimeout(timeoutId));

      return () => {
        clearTimeout(timeoutId);
        controller.abort();
      };
    });
  }

  private parsearEvento(bloque: string): { tipo: string; texto: string } {
    let tipo = 'message';
    const datos: string[] = [];
    for (const linea of bloque.split('\n')) {
      if (linea.startsWith('event:')) tipo = linea.slice(6).trim();
      else if (linea.startsWith('data:')) datos.push(linea.slice(5));
    }
    if (datos.length === 0) return { tipo, texto: '' };
    return { tipo, texto: JSON.parse(datos.join('\n')).texto ?? '' };
  }
}
