import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { loadStripe } from '@stripe/stripe-js';

@Injectable({
  providedIn: 'root',
})
export class PaymentService {
  private backendUrl = 'http://localhost:8090/api/pagos';

  constructor(private http: HttpClient) {}

  async crearSesionPago(monto: number) {
    const stripe = await loadStripe('pk_test_51RMlUFQNo8xsvCpZME2OZqNMpMCDbAPBfvNnynCZnmcYdxfX6th2CqBYlHWo2OU6yjn580mUWhC0ODSILgJGaikV00ckBIZKmb');

    const token = localStorage.getItem('token'); // Recupera el JWT almacenado

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    this.http
      .post<{ url: string }>(
        `${this.backendUrl}/crear-sesion`,
        { monto },
        { headers }  // ✅ Añade headers con token
      )
      .subscribe(async (res) => {
        if (res.url) {
          window.location.href = res.url;
        }
      }, err => {
        console.error('Error al crear la sesión de pago:', err);
      });
  }
}
