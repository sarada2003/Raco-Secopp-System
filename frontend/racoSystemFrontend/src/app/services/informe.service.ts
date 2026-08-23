import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InformeDetalleDTO, InformeResumenDTO } from '../Dtos/informe.dto';

@Injectable({ providedIn: 'root' })
export class InformeService {

  private readonly api = 'http://localhost:8090/api/informes';

  constructor(private http: HttpClient) {}

  listar(): Observable<InformeResumenDTO[]> {
    return this.http.get<InformeResumenDTO[]>(this.api);
  }

  obtener(id: number): Observable<InformeDetalleDTO> {
    return this.http.get<InformeDetalleDTO>(`${this.api}/${id}`);
  }
}
