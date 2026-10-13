import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ChatbotService, ErrorAsistente } from 'src/app/services/chatbot.service';

interface Mensaje {
  origen: 'usuario' | 'bot';
  texto: string;
  hora: string;
}

@Component({
  selector: 'app-chatbot',
  templateUrl: './chatbot.component.html',
  styleUrls: ['./chatbot.component.scss']
})
export class ChatbotComponent implements OnInit, AfterViewChecked {

  @ViewChild('messagesEnd') messagesEnd!: ElementRef;

  mensajes: Mensaje[] = [];
  inputTexto = '';
  /** Puntos de "escribiendo": solo mientras aún no llega el primer fragmento. */
  botEscribiendo = false;
  /** Hay una respuesta en curso (bloquea el envío de otro mensaje hasta que termine). */
  respondiendo = false;
  private pendingScroll = false;

  constructor(private chatbotService: ChatbotService) {}

  ngOnInit(): void {
    this.mensajes = [{
      origen: 'bot',
      texto: '¡Bienvenido al Chatbot RACO! Soy tu asistente para consultas sobre contratación pública en Colombia. Puedes preguntarme sobre SECOP, contratos, licitaciones o cómo usar el sistema.',
      hora: this.horaActual()
    }];
    this.pendingScroll = true;
  }

  ngAfterViewChecked(): void {
    if (this.pendingScroll) {
      this.scrollAlFinal();
      this.pendingScroll = false;
    }
  }

  enviar(): void {
    const texto = this.inputTexto.trim();
    if (!texto || this.respondiendo) return;

    this.mensajes.push({ origen: 'usuario', texto, hora: this.horaActual() });
    this.inputTexto = '';
    this.respondiendo = true;
    this.botEscribiendo = true;
    this.pendingScroll = true;

    // La burbuja del bot se crea con el primer fragmento; hasta entonces se ven los puntos de "escribiendo".
    let respuesta: Mensaje | null = null;

    this.chatbotService.chatStream(texto).subscribe({
      next: (fragmento) => {
        if (!respuesta) {
          respuesta = { origen: 'bot', texto: '', hora: this.horaActual() };
          this.mensajes.push(respuesta);
          this.botEscribiendo = false;
        }
        respuesta.texto += fragmento;
        this.pendingScroll = true;
      },
      complete: () => this.terminarRespuesta(),
      error: (err) => {
        const aviso = err instanceof ErrorAsistente
          ? err.message
          : 'El asistente no respondió a tiempo. Verifica tu conexión e intenta de nuevo.';
        if (respuesta) {
          respuesta.texto += `\n\n_${aviso}_`;
        } else {
          this.mensajes.push({ origen: 'bot', texto: aviso, hora: this.horaActual() });
        }
        this.terminarRespuesta();
      }
    });
  }

  private terminarRespuesta(): void {
    this.respondiendo = false;
    this.botEscribiendo = false;
    this.pendingScroll = true;
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }

  private horaActual(): string {
    return new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
  }

  private scrollAlFinal(): void {
    try { this.messagesEnd?.nativeElement?.scrollIntoView({ behavior: 'smooth' }); } catch {}
  }
}
