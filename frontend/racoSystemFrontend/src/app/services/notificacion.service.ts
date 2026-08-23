import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificacionDTO, SuscripcionDTO, SuscripcionRequestDTO } from '../Dtos/notificacion.dto';

@Injectable({ providedIn: 'root' })
export class NotificacionService {

  private readonly api = 'http://localhost:8090/api/notificaciones';

  constructor(private http: HttpClient) {}

  listar(): Observable<NotificacionDTO[]> {
    return this.http.get<NotificacionDTO[]>(this.api);
  }

  contarNoLeidas(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.api}/no-leidas/count`);
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.patch<void>(`${this.api}/${id}/leida`, {});
  }

  marcarTodasLeidas(): Observable<void> {
    return this.http.patch<void>(`${this.api}/leidas`, {});
  }

  crearSuscripcion(request: SuscripcionRequestDTO): Observable<any> {
    return this.http.post(`${this.api}/suscripciones`, request);
  }

  listarSuscripciones(): Observable<SuscripcionDTO[]> {
    return this.http.get<SuscripcionDTO[]>(`${this.api}/suscripciones`);
  }

  eliminarSuscripcion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/suscripciones/${id}`);
  }
}
