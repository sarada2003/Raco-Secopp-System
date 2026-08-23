import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { UsuarioDTO } from 'src/app/Dtos/usuario.dto';
import { AuthService } from 'src/app/services/auth.service';
import { NotificacionService } from 'src/app/services/notificacion.service';

const INTERVALO_POLL_NOTIFICACIONES_MS = 60_000;

@Component({
  selector: 'app-dashboard-usuario',
  templateUrl: './dashboard-usuario.component.html',
  styleUrls: ['./dashboard-usuario.component.scss']
})
export class DashboardUsuarioComponent implements OnInit, OnDestroy {

  nombreUsuario: string = '';
  notificacionesNoLeidas = 0;

  private pollSub?: Subscription;

  constructor(
    private authService: AuthService,
    private notificacionService: NotificacionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.getUsuarioActual().subscribe({
      next: (usuario: UsuarioDTO) => {
        this.nombreUsuario = usuario.email;
      },
      error: (err) => {
        console.error('Error obteniendo usuario:', err);
      }
    });

    this.pollSub = interval(INTERVALO_POLL_NOTIFICACIONES_MS).pipe(
      startWith(0),
      switchMap(() => this.notificacionService.contarNoLeidas())
    ).subscribe({
      next: (res) => { this.notificacionesNoLeidas = res.count; },
      error: (err) => console.error('Error obteniendo notificaciones no leídas:', err)
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/home']);
  }

}
