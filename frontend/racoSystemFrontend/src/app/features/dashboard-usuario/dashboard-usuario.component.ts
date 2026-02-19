import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { UsuarioDTO } from 'src/app/Dtos/usuario.dto';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-dashboard-usuario',
  templateUrl: './dashboard-usuario.component.html',
  styleUrls: ['./dashboard-usuario.component.scss']
})
export class DashboardUsuarioComponent {



  nombreUsuario: string = '';

constructor(private authService: AuthService, private router: Router) {}

ngOnInit(): void {
    this.authService.getUsuarioActual().subscribe({
      next: (usuario: UsuarioDTO) => {
        this.nombreUsuario = usuario.email;
      },
      error: (err) => {
        console.error('Error obteniendo usuario:', err);
      }
    });
  }


  logout(): void {
  this.authService.logout();
  this.router.navigate(['/home']);
}

}
