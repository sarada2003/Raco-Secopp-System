import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { timeout } from 'rxjs/operators';
import { SecopContratoDTO } from '../Dtos/secop-contrato.dto';
import { ContractSearchRequestDTO } from '../Dtos/contract-search-request.dto';

@Injectable({ providedIn: 'root' })
export class SecopService {

  private readonly api = 'http://localhost:8090/api/secop';
  private readonly TIMEOUT_MS = 55_000;

  constructor(private http: HttpClient) {}

  obtenerContratos(page: number = 1, size: number = 10): Observable<SecopContratoDTO[]> {
    return this.http.get<SecopContratoDTO[]>(`${this.api}/contratos`, {
      params: { page: page.toString(), size: size.toString() }
    }).pipe(timeout(this.TIMEOUT_MS));
  }

  buscarContratos(request: ContractSearchRequestDTO): Observable<SecopContratoDTO[]> {
    return this.http.post<SecopContratoDTO[]>(`${this.api}/buscar`, request)
      .pipe(timeout(this.TIMEOUT_MS));
  }
}
