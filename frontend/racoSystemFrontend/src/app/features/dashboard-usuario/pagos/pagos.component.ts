import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-pagos',
  templateUrl: './pagos.component.html',
  styleUrls: ['./pagos.component.scss']
})
export class PagosComponent implements OnInit {
  montoFondeo: number = 0;
  private backendUrl = 'http://localhost:8090/api/pagos';

  constructor(private http: HttpClient, private router: Router ,private route: ActivatedRoute) {}

ngOnInit(): void {
  this.route.url.subscribe(segments => {
    const last = segments[segments.length - 1]?.path;
    if (last === 'pagos-success') {
      Swal.fire({
        icon: 'success',
        title: '¡Pago exitoso!',
        text: 'Tu pago fue procesado correctamente.'
      });
    } else if (last === 'pagos-cancel') {
      Swal.fire({
        icon: 'info',
        title: 'Pago cancelado',
        text: 'No se completó la operación.'
      });
    }
  });
}

  fondearCuenta() {
    if (!this.montoFondeo || this.montoFondeo < 1) {
      Swal.fire({
        icon: 'warning',
        title: 'Monto inválido',
        text: 'El monto mínimo para fondear es $1 USD'
      });
      return;
    }

    const payload = { monto: this.montoFondeo };

    this.http.post<any>(`${this.backendUrl}/crear-sesion`, payload).subscribe({
      next: (res) => {
        if (res.url) {
          window.location.href = res.url;
        } else {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se recibió una URL válida de Stripe.'
          });
        }
      },
      error: (err) => {
        console.error('Error al fondear:', err);
        Swal.fire({
          icon: 'error',
          title: 'Error en el pago',
          text: 'Hubo un problema al crear la sesión de pago.'
        });
      }
    });
  }

  suscribirse() {
    const payload = { monto: 20.0 };

    this.http.post<any>(`${this.backendUrl}/crear-sesion`, payload).subscribe({
      next: (res) => {
        if (res.url) {
          window.location.href = res.url;
        } else {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se recibió una URL válida de Stripe.'
          });
        }
      },
      error: (err) => {
        console.error('Error al suscribirse:', err);
        Swal.fire({
          icon: 'error',
          title: 'Error en el pago',
          text: 'Hubo un problema al crear la sesión de suscripción.'
        });
      }
    });
  }
}
