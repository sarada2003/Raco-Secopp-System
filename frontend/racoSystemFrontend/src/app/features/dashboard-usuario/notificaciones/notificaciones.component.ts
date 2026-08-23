import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { NotificacionService } from 'src/app/services/notificacion.service';
import { NotificacionDTO, SuscripcionDTO } from 'src/app/Dtos/notificacion.dto';

type Categoria = 'todas' | 'contrato' | 'alerta' | 'sistema';

const ICONOS_POR_CATEGORIA: Record<string, string> = {
  contrato: 'description',
  alerta: 'warning',
  sistema: 'info'
};

@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.component.html',
  styleUrls: ['./notificaciones.component.scss']
})
export class NotificacionesComponent implements OnInit {

  filtroActivo: Categoria = 'todas';
  notificaciones: NotificacionDTO[] = [];
  cargando = false;
  error = '';

  // ---- Configuración de alertas (pestaña "Sistema") ----
  criteriosForm!: FormGroup;
  frecuenciaAlerta: 'diario' | 'semanal' = 'diario';
  suscripciones: SuscripcionDTO[] = [];
  cargandoSuscripciones = false;
  guardandoAlerta = false;
  mensajeAlerta = '';
  mensajeAlertaTipo: 'ok' | 'error' = 'ok';

  readonly departamentos = [
    'Amazonas', 'Antioquia', 'Arauca', 'Atlántico', 'Bolívar', 'Boyacá',
    'Caldas', 'Caquetá', 'Casanare', 'Cauca', 'Cesar', 'Chocó', 'Córdoba',
    'Cundinamarca', 'Guainía', 'Guaviare', 'Huila', 'La Guajira', 'Magdalena',
    'Meta', 'Nariño', 'Norte de Santander', 'Putumayo', 'Quindío', 'Risaralda',
    'San Andrés y Providencia', 'Santander', 'Sucre', 'Tolima', 'Valle del Cauca',
    'Vaupés', 'Vichada', 'Distrito Capital de Bogotá'
  ];

  readonly modalidades = [
    'Licitación Pública',
    'Selección Abreviada de Menor Cuantía',
    'Contratación directa',
    'Concurso de Méritos',
    'Mínima Cuantía',
    'Contratación régimen especial'
  ];

  readonly estados = [
    'Publicado', 'Adjudicado', 'Celebrado', 'Desierto', 'Liquidado', 'Terminado'
  ];

  readonly tiposContrato = [
    'Obra', 'Prestación de servicios', 'Compraventa',
    'Consultoría', 'Suministro', 'Arrendamiento', 'Otro'
  ];

  constructor(private fb: FormBuilder, private notificacionService: NotificacionService) {}

  ngOnInit(): void {
    this.criteriosForm = this.fb.group({
      palabraClave: [''],
      entidad: [''],
      departamento: [''],
      modalidad: [''],
      estado: [''],
      tipoContrato: [''],
      valorMin: [null],
      valorMax: [null]
    });

    this.cargar();
    this.cargarSuscripciones();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.notificacionService.listar().subscribe({
      next: (data) => {
        this.notificaciones = data;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar las notificaciones. Intenta de nuevo.';
        this.cargando = false;
      }
    });
  }

  get filtradas(): NotificacionDTO[] {
    if (this.filtroActivo === 'todas') return this.notificaciones;
    return this.notificaciones.filter(n => n.categoria === this.filtroActivo);
  }

  get noLeidas(): number {
    return this.notificaciones.filter(n => !n.leida).length;
  }

  marcarLeida(n: NotificacionDTO): void {
    if (n.leida) return;
    n.leida = true; // optimista: revertir si falla
    this.notificacionService.marcarLeida(n.id).subscribe({
      error: () => { n.leida = false; }
    });
  }

  marcarTodasLeidas(): void {
    const previas = this.notificaciones.map(n => n.leida);
    this.notificaciones.forEach(n => n.leida = true);
    this.notificacionService.marcarTodasLeidas().subscribe({
      error: () => {
        this.notificaciones.forEach((n, i) => n.leida = previas[i]);
      }
    });
  }

  tituloDe(n: NotificacionDTO): string {
    return `Nuevo contrato publicado: ${n.nombreContrato || 'Proceso SECOP II'}`;
  }

  descripcionDe(n: NotificacionDTO): string {
    const entidad = n.entidadContrato || 'Una entidad';
    const valor = n.valorContrato ? ` por valor de ${this.formatearValor(n.valorContrato)}` : '';
    return `${entidad} publicó un proceso${valor}.`;
  }

  iconoDe(n: NotificacionDTO): string {
    return ICONOS_POR_CATEGORIA[n.categoria] || 'notifications';
  }

  formatearValor(valor: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency', currency: 'COP', maximumFractionDigits: 0
    }).format(valor);
  }

  formatearFecha(fecha: string): string {
    const d = new Date(fecha);
    const ahora = new Date();
    const diff = Math.floor((ahora.getTime() - d.getTime()) / 1000 / 60);
    if (diff < 60) return `Hace ${diff} min`;
    if (diff < 1440) return `Hace ${Math.floor(diff / 60)}h`;
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
  }

  // ---- Configuración de alertas ----

  cargarSuscripciones(): void {
    this.cargandoSuscripciones = true;
    this.notificacionService.listarSuscripciones().subscribe({
      next: (data) => {
        this.suscripciones = data;
        this.cargandoSuscripciones = false;
      },
      error: () => { this.cargandoSuscripciones = false; }
    });
  }

  guardarAlerta(): void {
    if (this.guardandoAlerta) return;
    this.guardandoAlerta = true;
    this.mensajeAlerta = '';

    const v = this.criteriosForm.value;
    this.notificacionService.crearSuscripcion({
      criterio: {
        palabraClave: v.palabraClave?.trim() || undefined,
        entidad:      v.entidad?.trim()      || undefined,
        departamento: v.departamento         || undefined,
        modalidad:    v.modalidad            || undefined,
        estado:       v.estado               || undefined,
        tipoContrato: v.tipoContrato         || undefined,
        valorMin:     v.valorMin             || undefined,
        valorMax:     v.valorMax             || undefined
      },
      frecuencia: this.frecuenciaAlerta
    }).subscribe({
      next: () => {
        this.mensajeAlertaTipo = 'ok';
        this.mensajeAlerta = 'Alerta guardada. Te avisaremos cuando aparezcan contratos nuevos con estos filtros.';
        this.guardandoAlerta = false;
        this.criteriosForm.reset();
        this.cargarSuscripciones();
      },
      error: () => {
        this.mensajeAlertaTipo = 'error';
        this.mensajeAlerta = 'No se pudo guardar la alerta. Intenta de nuevo.';
        this.guardandoAlerta = false;
      }
    });
  }

  eliminarSuscripcion(s: SuscripcionDTO): void {
    const previas = this.suscripciones;
    this.suscripciones = this.suscripciones.filter(x => x.id !== s.id); // optimista
    this.notificacionService.eliminarSuscripcion(s.id).subscribe({
      error: () => { this.suscripciones = previas; }
    });
  }

  resumenCriterio(s: SuscripcionDTO): string {
    const c = s.criterio;
    const partes = [
      c.palabraClave && `"${c.palabraClave}"`,
      c.entidad,
      c.departamento,
      c.modalidad,
      c.estado,
      c.tipoContrato,
      c.valorMin ? `desde ${this.formatearValor(c.valorMin)}` : null,
      c.valorMax ? `hasta ${this.formatearValor(c.valorMax)}` : null
    ].filter(Boolean);
    return partes.length ? partes.join(' · ') : 'Todos los contratos';
  }
}
