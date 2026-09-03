import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { Auth } from '@angular/fire/auth';

import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: Auth, useValue: {} },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) }
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('isAuthenticated', () => {
    it('devuelve true cuando hay un token guardado', () => {
      localStorage.setItem('token', 'jwt-de-prueba');
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('devuelve false cuando no hay token', () => {
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  describe('loginManual', () => {
    it('hace POST a /api/auth/login con las credenciales', () => {
      service.loginManual('usuario@correo.com', 'clave123').subscribe();

      const req = httpMock.expectOne('http://localhost:8090/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'usuario@correo.com', password: 'clave123' });
      req.flush({ token: 'jwt', usuario: {} });
    });
  });

  describe('getUsuarioActual', () => {
    it('adjunta el token guardado como header Authorization', () => {
      localStorage.setItem('token', 'jwt-de-prueba');

      service.getUsuarioActual().subscribe();

      const req = httpMock.expectOne('http://localhost:8090/api/auth/me');
      expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-de-prueba');
      req.flush({});
    });
  });
});
