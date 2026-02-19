import { Component, OnInit } from '@angular/core';
import { AuthService } from 'src/app/services/auth.service';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import Swal from 'sweetalert2';



@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit{
    loginForm!: FormGroup;
    username = '';
     password = '';

  constructor(
    private authService: AuthService,
    private http: HttpClient,
    private router: Router,
     private fb: FormBuilder,
  ) {}


    ngOnInit(): void {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

loginGoogle() {
  this.authService.loginWithGoogle()
    .then(userCredential => {
      const email = userCredential.user.email;
      console.log('Sesión iniciada con:', email);

      this.http.post<any>('http://localhost:8090/api/auth/login', {
        email: email,
        password: '' // si no usas contraseña aquí, asegúrate que tu backend lo acepte solo con email
      }).subscribe({
        next: (res) => {
          localStorage.setItem('token', res.token);
          this.router.navigate(['/dashboard']);
        },
        error: () => {
          alert('Este correo no está registrado en la plataforma.');
          this.authService.logout();
        }
      });
    })
    .catch(error => {
      console.error('Error en el inicio de sesión', error);
      alert('Error al iniciar sesión');
    });
}


   onLogin(): void {
    if (this.loginForm.valid) {
      const { username, password } = this.loginForm.value;

      this.authService.loginManual(username, password).subscribe({
        next: (res) => {
          localStorage.setItem('token', res.token); // ✅ Guarda el token
          Swal.fire({
            icon: 'success',
            title: 'Inicio de sesión exitoso',
            text: `¡Bienvenido ${res.usuario}!`,
            timer: 2000,
            showConfirmButton: false
          });
          this.router.navigate(['/dashboard']);
        },
        error: () => {
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Credenciales incorrectas o usuario no encontrado'
          });
        }
      });
    }
  }

}
