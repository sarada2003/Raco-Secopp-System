import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-dashboard-inicio',
  templateUrl: './inicio.component.html',
  styleUrls: ['./inicio.component.scss']
})
export class DashboardInicioComponent implements OnInit {

  nombreUsuario = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.authService.getUsuarioActual().subscribe({
      next: (u) => { this.nombreUsuario = u.nombre || u.email; },
      error: () => { this.nombreUsuario = 'Usuario'; }
    });
  }

  ir(ruta: string): void {
    this.router.navigate(['/dashboard', ruta]);
  }
}
