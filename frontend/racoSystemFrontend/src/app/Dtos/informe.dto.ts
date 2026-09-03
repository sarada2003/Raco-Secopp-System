import { ContractSearchRequestDTO } from './contract-search-request.dto';
import { SecopContratoDTO } from './secop-contrato.dto';

export interface InformeResumenDTO {
  id: number;
  nombre: string;
  fecha: string;
  cantidadResultados: number;
  origen: 'MANUAL' | 'CHATBOT';
}

export interface InformeDetalleDTO extends InformeResumenDTO {
  criterio: ContractSearchRequestDTO;
  resultados: SecopContratoDTO[];
}
