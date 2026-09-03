import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { SecopService } from './secop.service';
import { ContractSearchRequestDTO } from '../Dtos/contract-search-request.dto';

describe('SecopService', () => {
  let service: SecopService;
  let httpMock: HttpTestingController;
  const api = 'http://localhost:8090/api/secop';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(SecopService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('obtenerContratos', () => {
    it('hace GET a /contratos con page y size como query params', () => {
      service.obtenerContratos(2, 5).subscribe(res => expect(res).toEqual([]));

      const req = httpMock.expectOne(r => r.url === `${api}/contratos`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('2');
      expect(req.request.params.get('size')).toBe('5');
      req.flush([]);
    });

    it('usa page=1 y size=10 por defecto', () => {
      service.obtenerContratos().subscribe();

      const req = httpMock.expectOne(r => r.url === `${api}/contratos`);
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('10');
      req.flush([]);
    });
  });

  describe('buscarContratos', () => {
    it('hace POST a /buscar con los filtros en el body', () => {
      const filtros: ContractSearchRequestDTO = { palabraClave: 'obras', page: 0, size: 10 };

      service.buscarContratos(filtros).subscribe(res => expect(res).toEqual([]));

      const req = httpMock.expectOne(`${api}/buscar`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(filtros);
      req.flush([]);
    });
  });
});
