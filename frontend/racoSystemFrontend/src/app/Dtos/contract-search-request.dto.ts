export interface ContractSearchRequestDTO {
  palabraClave?: string;
  entidad?: string;
  departamento?: string;
  modalidad?: string;
  estado?: string;
  tipoContrato?: string;
  valorMin?: number;
  valorMax?: number;
  fechaFirmaDesde?: string;
  fechaFirmaHasta?: string;
  ordenarPor?: string;
  page?: number;
  size?: number;
}
