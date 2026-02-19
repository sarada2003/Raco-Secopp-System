import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstrategiasComponent } from './estrategias.component';

describe('EstrategiasComponent', () => {
  let component: EstrategiasComponent;
  let fixture: ComponentFixture<EstrategiasComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EstrategiasComponent]
    });
    fixture = TestBed.createComponent(EstrategiasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
