// Campos reales del dataset p6dx-8zbt (SECOP II procesos de contratación)
export interface SecopContratoDTO {
  id_del_proceso?: string;
  entidad?: string;
  departamento_entidad?: string;
  ciudad_entidad?: string;
  nombre_del_procedimiento?: string;
  descripci_n_del_procedimiento?: string;
  modalidad_de_contratacion?: string;
  estado_del_procedimiento?: string;
  tipo_de_contrato?: string;
  valor_total_adjudicacion?: number;
  fecha_de_publicacion_del?: string;
  fecha_adjudicacion?: string;
  nombre_del_proveedor?: string;
  nit_del_proveedor_adjudicado?: string;
  urlproceso?: string;
}
