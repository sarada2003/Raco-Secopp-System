import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { GoogleAuthProvider, signInWithPopup, signOut, Auth } from '@angular/fire/auth';
import { Observable } from 'rxjs';
import { UsuarioDTO } from '../Dtos/usuario.dto';
import { UserCredential } from '@angular/fire/auth';


@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private backendUrl = 'http://localhost:8090/api/auth';

  constructor(
    private http: HttpClient,
    private auth: Auth,
    private router: Router
  ) {}

  
loginWithGoogle(): Promise<UserCredential> {
  const provider = new GoogleAuthProvider();
  return signInWithPopup(this.auth, provider);
}
  logout() {
    signOut(this.auth).then(() => {
      localStorage.removeItem('token');
      this.router.navigate(['/login']);
    });
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }


 getUsuarioActual(): Observable<UsuarioDTO> {
  const headers = new HttpHeaders({
    Authorization: `Bearer ${localStorage.getItem('token')}`
  });

  return this.http.get<UsuarioDTO>(`${this.backendUrl}/me`, { headers });
}

loginManual(email: string, password: string): Observable<{ token: string, usuario: UsuarioDTO }> {
  return this.http.post<{ token: string, usuario: UsuarioDTO }>(`${this.backendUrl}/login`, {
    email,
    password
  });
}

}
