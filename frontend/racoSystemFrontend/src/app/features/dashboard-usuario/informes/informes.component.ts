import { Component, OnInit } from '@angular/core';
import { jsPDF } from 'jspdf';
import { autoTable } from 'jspdf-autotable';
import { InformeService } from 'src/app/services/informe.service';
import { InformeDetalleDTO, InformeResumenDTO } from 'src/app/Dtos/informe.dto';
import { SecopContratoDTO } from 'src/app/Dtos/secop-contrato.dto';

interface BarraDato {
  label: string;
  valor: number;
  pct: number;
  valorLabel: string;
}

const MAX_BARRAS = 6;

@Component({
  selector: 'app-informes',
  templateUrl: './informes.component.html',
  styleUrls: ['./informes.component.scss']
})
export class InformesComponent implements OnInit {

  informes: InformeResumenDTO[] = [];
  cargandoInformes = false;
  error = '';

  informeActivo: InformeDetalleDTO | null = null;
  cargandoDetalle = false;
  informeActivoId: number | null = null; // para resaltar cuál se está viendo en la lista

  // ---- Agregados para las gráficas (calculados del snapshot del informe activo) ----
  kpiTotalContratos = 0;
  kpiValorTotal = 0;
  kpiDepartamentoLider = '—';
  kpiModalidadLider = '—';
  porDepartamento: BarraDato[] = [];
  porModalidad: BarraDato[] = [];
  porEstado: BarraDato[] = [];
  porEntidadValor: BarraDato[] = [];

  constructor(private informeService: InformeService) {}

  ngOnInit(): void {
    this.cargarInformes();
  }

  cargarInformes(): void {
    this.cargandoInformes = true;
    this.informeService.listar().subscribe({
      next: (data) => {
        this.informes = data;
        this.cargandoInformes = false;
        if (!this.informeActivo && data.length > 0) {
          this.verInforme(data[0]);
        }
      },
      error: () => { this.cargandoInformes = false; }
    });
  }

  verInforme(resumen: InformeResumenDTO): void {
    if (this.informeActivoId === resumen.id) return;
    this.cargandoDetalle = true;
    this.informeActivoId = resumen.id;
    this.informeService.obtener(resumen.id).subscribe({
      next: (detalle) => {
        this.informeActivo = detalle;
        this.cargandoDetalle = false;
        this.procesarAgregados(detalle.resultados);
      },
      error: () => {
        this.cargandoDetalle = false;
        this.error = 'No se pudo cargar el informe.';
      }
    });
  }

  descargarPDF(informe: InformeDetalleDTO | null): void {
    if (!informe) return;

    const doc = new jsPDF({ orientation: 'landscape' });
    doc.setFontSize(14);
    doc.text(informe.nombre, 14, 15);
    doc.setFontSize(9);
    doc.text(
      `Generado: ${this.formatearFecha(informe.fecha)} — ${informe.cantidadResultados} resultado(s)`,
      14, 21
    );

    autoTable(doc, {
      startY: 26,
      head: [['Entidad', 'Proceso', 'Departamento', 'Modalidad', 'Estado', 'Valor', 'Fecha publicación']],
      body: informe.resultados.map((c: SecopContratoDTO) => [
        c.entidad || '—',
        c.nombre_del_procedimiento || '—',
        c.departamento_entidad || '—',
        c.modalidad_de_contratacion || '—',
        c.estado_del_procedimiento || '—',
        c.valor_total_adjudicacion ? this.formatearValor(c.valor_total_adjudicacion) : 'Sin adjudicar',
        this.formatearFecha(c.fecha_de_publicacion_del || '')
      ]),
      styles: { fontSize: 7 },
      headStyles: { fillColor: [229, 196, 106], textColor: [0, 0, 0] }
    });

    const nombreArchivo = (informe.nombre || 'informe').replace(/[^\w\-]+/g, '_');
    doc.save(`${nombreArchivo}.pdf`);
  }

  descargarPDFDesdeLista(resumen: InformeResumenDTO): void {
    if (this.informeActivo && this.informeActivo.id === resumen.id) {
      this.descargarPDF(this.informeActivo);
      return;
    }
    this.informeService.obtener(resumen.id).subscribe({
      next: (detalle) => this.descargarPDF(detalle),
      error: () => { this.error = 'No se pudo descargar el informe.'; }
    });
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

  formatearValorCompacto(valor: number): string {
    if (!valor) return '$0';
    return '$' + new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 }).format(valor);
  }

  // ---- Agregación para las gráficas ----
  // Cada gráfica responde "comparar magnitud" (conteo o valor por categoría), así que
  // todas usan barras horizontales de un solo hue (el dorado del sistema) en vez de
  // colores categóricos — no hay identidad de serie que distinguir, solo cantidad.
  private procesarAgregados(resultados: SecopContratoDTO[]): void {
    this.kpiTotalContratos = resultados.length;
    this.kpiValorTotal = resultados.reduce((sum, c) => sum + (c.valor_total_adjudicacion || 0), 0);

    const conteoDepartamento = this.contarPor(resultados, c => c.departamento_entidad);
    const conteoModalidad = this.contarPor(resultados, c => c.modalidad_de_contratacion);
    const conteoEstado = this.contarPor(resultados, c => c.estado_del_procedimiento);
    const valorPorEntidad = this.sumarPor(resultados, c => c.entidad, c => c.valor_total_adjudicacion || 0);

    this.porDepartamento = this.topNConOtros(conteoDepartamento, MAX_BARRAS, v => `${v}`);
    this.porModalidad = this.topNConOtros(conteoModalidad, MAX_BARRAS, v => `${v}`);
    this.porEstado = this.topNConOtros(conteoEstado, MAX_BARRAS, v => `${v}`);
    this.porEntidadValor = this.topNConOtros(valorPorEntidad, MAX_BARRAS, v => this.formatearValor(v));

    const topDepartamento = [...conteoDepartamento.entries()].sort((a, b) => b[1] - a[1])[0];
    const topModalidad = [...conteoModalidad.entries()].sort((a, b) => b[1] - a[1])[0];
    this.kpiDepartamentoLider = topDepartamento ? `${topDepartamento[0]} (${topDepartamento[1]})` : '—';
    this.kpiModalidadLider = topModalidad ? `${topModalidad[0]} (${topModalidad[1]})` : '—';
  }

  private contarPor(resultados: SecopContratoDTO[], clave: (c: SecopContratoDTO) => string | undefined): Map<string, number> {
    const mapa = new Map<string, number>();
    for (const c of resultados) {
      const k = clave(c)?.trim() || 'Sin definir';
      mapa.set(k, (mapa.get(k) || 0) + 1);
    }
    return mapa;
  }

  private sumarPor(
    resultados: SecopContratoDTO[],
    clave: (c: SecopContratoDTO) => string | undefined,
    valor: (c: SecopContratoDTO) => number
  ): Map<string, number> {
    const mapa = new Map<string, number>();
    for (const c of resultados) {
      const k = clave(c)?.trim() || 'Sin definir';
      mapa.set(k, (mapa.get(k) || 0) + valor(c));
    }
    return mapa;
  }

  /** Top-N ordenado por magnitud descendente; el resto se pliega en "Otros" en vez de saturar la gráfica de barras. */
  private topNConOtros(mapa: Map<string, number>, n: number, formatear: (v: number) => string): BarraDato[] {
    const entradas = [...mapa.entries()].sort((a, b) => b[1] - a[1]);
    const top = entradas.slice(0, n);
    const resto = entradas.slice(n).reduce((sum, [, v]) => sum + v, 0);
    const items: [string, number][] = resto > 0 ? [...top, ['Otros', resto]] : top;
    const max = Math.max(...items.map(([, v]) => v), 1);
    return items.map(([label, valor]) => ({ label, valor, pct: (valor / max) * 100, valorLabel: formatear(valor) }));
  }
}
