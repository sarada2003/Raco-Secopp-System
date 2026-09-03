export interface NotificacionDTO {
  id: number;
  idProcesoContrato: string;
  nombreContrato?: string;
  entidadContrato?: string;
  valorContrato?: number;
  fechaPublicacionContrato?: string;
  urlProceso?: string;
  categoria: 'contrato' | 'alerta' | 'sistema';
  leida: boolean;
  creadoEn: string;
}

export interface CriterioAlertaDTO {
  palabraClave?: string;
  entidad?: string;
  departamento?: string;
  modalidad?: string;
  estado?: string;
  tipoContrato?: string;
  valorMin?: number;
  valorMax?: number;
}

export interface SuscripcionRequestDTO {
  criterio: CriterioAlertaDTO;
  frecuencia: 'diario' | 'semanal';
}

export interface SuscripcionDTO {
  id: number;
  criterio: CriterioAlertaDTO;
  frecuencia: 'diario' | 'semanal';
  ultimaEjecucion?: string;
  creadoEn: string;
}
