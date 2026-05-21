import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ChatbotService } from 'src/app/services/chatbot.service';

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
  botEscribiendo = false;
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
    if (!texto || this.botEscribiendo) return;

    this.mensajes.push({ origen: 'usuario', texto, hora: this.horaActual() });
    this.inputTexto = '';
    this.botEscribiendo = true;
    this.pendingScroll = true;

    this.chatbotService.chat(texto).subscribe({
      next: (respuesta) => {
        this.mensajes.push({ origen: 'bot', texto: respuesta, hora: this.horaActual() });
        this.botEscribiendo = false;
        this.pendingScroll = true;
      },
      error: () => {
        this.mensajes.push({
          origen: 'bot',
          texto: 'El asistente no respondió a tiempo. Verifica tu conexión e intenta de nuevo.',
          hora: this.horaActual()
        });
        this.botEscribiendo = false;
        this.pendingScroll = true;
      }
    });
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
