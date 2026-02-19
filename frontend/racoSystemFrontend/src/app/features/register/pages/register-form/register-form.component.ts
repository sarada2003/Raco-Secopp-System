import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import Swal from 'sweetalert2';
import { Router } from '@angular/router';

@Component({
  selector: 'app-register-form',
  templateUrl: './register-form.component.html',
  styleUrls: ['./register-form.component.scss']
})
export class RegisterFormComponent {
  registerForm: FormGroup;
  private backendUrl = 'http://localhost:8090/api/auth/register';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router
  ) {
    this.registerForm = this.fb.group({
      name: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\d{7,15}$/)]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/)
      ]]
    });
  }

  onSubmit() {
    if (this.registerForm.valid) {
      const formData = {
        nombre: this.registerForm.value.name,
        username: this.registerForm.value.username,
        email: this.registerForm.value.email,
        telefono: this.registerForm.value.phone,
        password: this.registerForm.value.password
      };

      this.http.post(this.backendUrl, formData).subscribe({
        next: () => {
          Swal.fire({
            icon: 'success',
            title: '¡Registro exitoso!',
            text: 'Tu cuenta ha sido creada correctamente.',
            showConfirmButton: true,
            confirmButtonText: 'Iniciar sesión',
            confirmButtonColor: '#000'
          }).then(() => {
            this.router.navigate(['/login']);
          });
        },
        error: (err) => {
          console.error(err);
          Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo completar el registro. Verifica los datos e intenta de nuevo.',
            confirmButtonColor: '#d33'
          });
        }
      });
    }
  }
}
