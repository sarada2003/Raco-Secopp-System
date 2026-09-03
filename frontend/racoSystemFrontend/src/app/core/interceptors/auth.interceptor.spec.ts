import { TestBed } from '@angular/core/testing';
import { HttpClient, HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthInterceptor } from './auth.interceptor';

describe('AuthInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthInterceptor,
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    const interceptor: AuthInterceptor = TestBed.inject(AuthInterceptor);
    expect(interceptor).toBeTruthy();
  });

  it('agrega el header Authorization cuando hay un token en localStorage', () => {
    localStorage.setItem('token', 'jwt-de-prueba');

    httpClient.get('/api/secop/contratos').subscribe();

    const req = httpMock.expectOne('/api/secop/contratos');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-de-prueba');
    req.flush({});
  });

  it('no agrega el header Authorization cuando no hay token', () => {
    httpClient.get('/api/secop/contratos').subscribe();

    const req = httpMock.expectOne('/api/secop/contratos');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
