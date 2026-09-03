import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Auth } from '@angular/fire/auth';
import { NO_ERRORS_SCHEMA } from '@angular/core';

import { DashboardUsuarioComponent } from './dashboard-usuario.component';

describe('DashboardUsuarioComponent', () => {
  let component: DashboardUsuarioComponent;
  let fixture: ComponentFixture<DashboardUsuarioComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule, RouterTestingModule,
        MatMenuModule, MatIconModule, MatToolbarModule, NoopAnimationsModule
      ],
      declarations: [DashboardUsuarioComponent],
      providers: [{ provide: Auth, useValue: {} }],
      schemas: [NO_ERRORS_SCHEMA] // template con múltiples subcomponentes (notificaciones, contratos, etc.), fuera de alcance en este smoke test
    });
    fixture = TestBed.createComponent(DashboardUsuarioComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
