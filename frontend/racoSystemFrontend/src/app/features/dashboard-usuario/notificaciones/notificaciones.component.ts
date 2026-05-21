import { Component, OnInit } from '@angular/core';

type Categoria = 'todas' | 'contrato' | 'alerta' | 'sistema';

interface Notificacion {
  id: number;
  titulo: string;
  descripcion: string;
  categoria: 'contrato' | 'alerta' | 'sistema';
  fecha: string;
  leida: boolean;
  icono: string;
}

@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.component.html',
  styleUrls: ['./notificaciones.component.scss']
})
export class NotificacionesComponent implements OnInit {

  filtroActivo: Categoria = 'todas';

  notificaciones: Notificacion[] = [
    {
      id: 1,
      titulo: 'Nuevo contrato publicado: consultoría TI',
      descripcion: 'El Ministerio de Tecnologías publicó un proceso de licitación para servicios de consultoría en transformación digital por valor de $500.000.000.',
      categoria: 'contrato',
      fecha: '2026-04-01T09:15:00',
      leida: false,
      icono: 'description'
    },
    {
      id: 2,
      titulo: 'Alerta: contrato próximo a vencer',
      descripcion: 'El proceso LP-001-2026 de la Alcaldía de Bogotá tiene plazo de cierre en las próximas 48 horas.',
      categoria: 'alerta',
      fecha: '2026-04-01T07:00:00',
      leida: false,
      icono: 'warning'
    },
    {
      id: 3,
      titulo: 'Actualización del sistema SECOP II',
      descripcion: 'Colombia Compra Eficiente realizó actualizaciones al sistema. Los datos de contratos pueden tardar hasta 2 horas en sincronizarse.',
      categoria: 'sistema',
      fecha: '2026-03-31T18:30:00',
      leida: true,
      icono: 'info'
    },
    {
      id: 4,
      titulo: 'Contrato adjudicado — obras civiles',
      descripcion: 'El proceso de selección abreviada para obras de mantenimiento vial en Cundinamarca fue adjudicado por $1.200.000.000.',
      categoria: 'contrato',
      fecha: '2026-03-31T14:00:00',
      leida: true,
      icono: 'description'
    },
    {
      id: 5,
      titulo: 'Nueva entidad registrada',
      descripcion: 'La Empresa de Servicios Públicos de Medellín se registró en SECOP II y comenzará a publicar sus procesos de contratación.',
      categoria: 'sistema',
      fecha: '2026-03-30T10:45:00',
      leida: true,
      icono: 'account_balance'
    }
  ];

  ngOnInit(): void {}

  get filtradas(): Notificacion[] {
    if (this.filtroActivo === 'todas') return this.notificaciones;
    return this.notificaciones.filter(n => n.categoria === this.filtroActivo);
  }

  get noLeidas(): number {
    return this.notificaciones.filter(n => !n.leida).length;
  }

  marcarLeida(n: Notificacion): void {
    n.leida = true;
  }

  marcarTodasLeidas(): void {
    this.notificaciones.forEach(n => n.leida = true);
  }

  formatearFecha(fecha: string): string {
    const d = new Date(fecha);
    const ahora = new Date();
    const diff = Math.floor((ahora.getTime() - d.getTime()) / 1000 / 60);
    if (diff < 60) return `Hace ${diff} min`;
    if (diff < 1440) return `Hace ${Math.floor(diff / 60)}h`;
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });
  }
}
