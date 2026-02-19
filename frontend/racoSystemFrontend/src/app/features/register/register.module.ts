import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RegisterFormComponent } from './pages/register-form/register-form.component';
import { RegisterRoutingModule } from './register-routing.module';

@NgModule({
  declarations: [RegisterFormComponent],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    HttpClientModule,   
    MatInputModule,
    MatButtonModule,
    RegisterRoutingModule,
    ReactiveFormsModule,
    FormsModule
  ]
})
export class RegisterModule { }
