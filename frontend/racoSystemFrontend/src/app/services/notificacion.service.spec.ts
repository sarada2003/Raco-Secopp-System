import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { NotificacionService } from './notificacion.service';
import { SuscripcionRequestDTO } from '../Dtos/notificacion.dto';

describe('NotificacionService', () => {
  let service: NotificacionService;
  let httpMock: HttpTestingController;
  const api = 'http://localhost:8090/api/notificaciones';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(NotificacionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('listar hace GET a la raíz del recurso', () => {
    service.listar().subscribe(res => expect(res).toEqual([]));

    const req = httpMock.expectOne(api);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('contarNoLeidas hace GET a /no-leidas/count', () => {
    service.contarNoLeidas().subscribe(res => expect(res).toEqual({ count: 3 }));

    const req = httpMock.expectOne(`${api}/no-leidas/count`);
    expect(req.request.method).toBe('GET');
    req.flush({ count: 3 });
  });

  it('marcarLeida hace PATCH a /{id}/leida', () => {
    service.marcarLeida(42).subscribe();

    const req = httpMock.expectOne(`${api}/42/leida`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('marcarTodasLeidas hace PATCH a /leidas', () => {
    service.marcarTodasLeidas().subscribe();

    const req = httpMock.expectOne(`${api}/leidas`);
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('crearSuscripcion hace POST a /suscripciones con el criterio y la frecuencia', () => {
    const request: SuscripcionRequestDTO = { criterio: { palabraClave: 'obras' }, frecuencia: 'diario' };

    service.crearSuscripcion(request).subscribe();

    const req = httpMock.expectOne(`${api}/suscripciones`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({});
  });

  it('listarSuscripciones hace GET a /suscripciones', () => {
    service.listarSuscripciones().subscribe(res => expect(res).toEqual([]));

    const req = httpMock.expectOne(`${api}/suscripciones`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('eliminarSuscripcion hace DELETE a /suscripciones/{id}', () => {
    service.eliminarSuscripcion(7).subscribe();

    const req = httpMock.expectOne(`${api}/suscripciones/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
