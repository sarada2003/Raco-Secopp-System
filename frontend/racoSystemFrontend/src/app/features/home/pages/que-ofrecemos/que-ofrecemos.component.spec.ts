import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QueOfrecemosComponent } from './que-ofrecemos.component';

describe('QueOfrecemosComponent', () => {
  let component: QueOfrecemosComponent;
  let fixture: ComponentFixture<QueOfrecemosComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [QueOfrecemosComponent]
    });
    fixture = TestBed.createComponent(QueOfrecemosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
