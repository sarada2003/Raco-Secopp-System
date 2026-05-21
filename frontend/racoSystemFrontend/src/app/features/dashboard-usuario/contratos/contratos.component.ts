import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { SecopService } from 'src/app/services/secop.service';
import { SecopContratoDTO } from 'src/app/Dtos/secop-contrato.dto';

@Component({
  selector: 'app-contratos',
  templateUrl: './contratos.component.html',
  styleUrls: ['./contratos.component.scss']
})
export class ContratosComponent implements OnInit {

  filterForm!: FormGroup;
  contratos: SecopContratoDTO[] = [];
  cargando = false;
  error = '';
  paginaActual = 0;
  hayMas = true;

  readonly pageSize = 20;

  readonly departamentos = [
    'Amazonas', 'Antioquia', 'Arauca', 'Atlántico', 'Bolívar', 'Boyacá',
    'Caldas', 'Caquetá', 'Casanare', 'Cauca', 'Cesar', 'Chocó', 'Córdoba',
    'Cundinamarca', 'Guainía', 'Guaviare', 'Huila', 'La Guajira', 'Magdalena',
    'Meta', 'Nariño', 'Norte de Santander', 'Putumayo', 'Quindío', 'Risaralda',
    'San Andrés y Providencia', 'Santander', 'Sucre', 'Tolima', 'Valle del Cauca',
    'Vaupés', 'Vichada', 'Bogotá D.C.'
  ];

  readonly modalidades = [
    'Licitación Pública',
    'Selección Abreviada de Menor Cuantía',
    'Contratación directa',
    'Concurso de Méritos',
    'Mínima Cuantía',
    'Contratación régimen especial'
  ];

  // Valores reales del campo estado_del_procedimiento en el dataset p6dx-8zbt
  readonly estados = [
    'Publicado', 'Adjudicado', 'Celebrado', 'Desierto', 'Liquidado', 'Terminado'
  ];

  readonly tiposContrato = [
    'Obra', 'Prestación de servicios', 'Compraventa',
    'Consultoría', 'Suministro', 'Arrendamiento', 'Otro'
  ];

  readonly ordenOptions = [
    { value: 'fecha_reciente', label: 'Fecha de firma (más reciente)' },
    { value: 'fecha_antigua',  label: 'Fecha de firma (más antiguo)' },
    { value: 'valor_mayor',    label: 'Valor (mayor a menor)' },
    { value: 'valor_menor',    label: 'Valor (menor a mayor)' }
  ];

  constructor(private fb: FormBuilder, private secopService: SecopService) {}

  ngOnInit(): void {
    this.filterForm = this.fb.group({
      palabraClave:    [''],
      entidad:         [''],
      departamento:    [''],
      modalidad:       [''],
      estado:          [''],
      tipoContrato:    [''],
      valorMin:        [null],
      valorMax:        [null],
      fechaFirmaDesde: [''],
      fechaFirmaHasta: [''],
      ordenarPor:      ['fecha_reciente']
    });
    this.buscar();
  }

  buscar(resetPage = true): void {
    if (resetPage) this.paginaActual = 0;
    this.cargando = true;
    this.error = '';

    const v = this.filterForm.value;

    this.secopService.buscarContratos({
      palabraClave:    v.palabraClave?.trim()    || undefined,
      entidad:         v.entidad?.trim()         || undefined,
      departamento:    v.departamento            || undefined,
      modalidad:       v.modalidad               || undefined,
      estado:          v.estado                  || undefined,
      tipoContrato:    v.tipoContrato            || undefined,
      valorMin:        v.valorMin                || undefined,
      valorMax:        v.valorMax                || undefined,
      fechaFirmaDesde: v.fechaFirmaDesde         || undefined,
      fechaFirmaHasta: v.fechaFirmaHasta         || undefined,
      ordenarPor:      v.ordenarPor              || undefined,
      page:            this.paginaActual,
      size:            this.pageSize
    }).subscribe({
      next: (data) => {
        this.contratos = data;
        this.hayMas = data.length === this.pageSize;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudo conectar con el servidor o la consulta tardó demasiado. Intenta de nuevo.';
        this.cargando = false;
      }
    });
  }

  paginaAnterior(): void {
    if (this.paginaActual === 0) return;
    this.paginaActual--;
    this.buscar(false);
  }

  paginaSiguiente(): void {
    this.paginaActual++;
    this.buscar(false);
  }

  limpiar(): void {
    this.filterForm.reset({ ordenarPor: 'fecha_reciente' });
    this.buscar();
  }

  getEstadoClass(estado: string): string {
    switch (estado?.toLowerCase()) {
      case 'adjudicado':  return 'estado-activo';
      case 'celebrado':   return 'estado-activo';
      case 'publicado':   return 'estado-aprobacion';
      case 'liquidado':   return 'estado-liquidado';
      case 'terminado':   return 'estado-terminado';
      case 'desierto':    return 'estado-cancelado';
      default:            return 'estado-default';
    }
  }

  formatearValor(valor: number): string {
    if (!valor) return '—';
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', maximumFractionDigits: 0
    }).format(valor);
  }

  formatearFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-CO', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }
}
