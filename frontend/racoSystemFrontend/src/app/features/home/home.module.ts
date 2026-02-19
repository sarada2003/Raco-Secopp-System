import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeComponent } from './home.component';
import { HomeRoutingModule } from './home-routing.module';
import { MatButtonModule } from '@angular/material/button';
import { QueOfrecemosComponent } from './pages/que-ofrecemos/que-ofrecemos.component';
import { EstrategiasComponent } from './pages/funcionamiento/funcionamiento';
import { SuscripcionComponent } from './pages/notificaciones/suscripcion.component';

@NgModule({
  declarations: [
    HomeComponent, 
    QueOfrecemosComponent, 
    EstrategiasComponent, SuscripcionComponent],
  imports: [
    CommonModule,
    MatButtonModule,
    HomeRoutingModule
  ]
})
export class HomeModule { }
