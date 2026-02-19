import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { QueOfrecemosComponent } from './features/home/pages/que-ofrecemos/que-ofrecemos.component';
import { SuscripcionComponent } from './features/home/pages/notificaciones/suscripcion.component';
import { LoginComponent } from './features/login/login.component';
import { AuthGuard } from './core/guards/auth.guard';
import { DashboardUsuarioComponent } from './features/dashboard-usuario/dashboard-usuario.component';
import { PagosComponent } from './features/dashboard-usuario/pagos/pagos.component';
import { EstrategiasComponent } from './features/home/pages/funcionamiento/estrategias.component';


const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: HomeComponent },
      { path: 'que-ofrecemos', component: QueOfrecemosComponent },
      { path: 'estrategias', component: EstrategiasComponent },
      { path: 'gold', component: SuscripcionComponent },
      {
        path: 'register',
        loadChildren: () =>
          import('./features/register/register.module').then(m => m.RegisterModule)
      },
      { path: 'login', component: LoginComponent },
    ]
  },
  {
    path: 'dashboard',
    component: DashboardUsuarioComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: HomeComponent },
      { path: 'pagos', component: PagosComponent },
      { path: 'perfil', component: HomeComponent },
      { path: 'estadisticas', component: HomeComponent },
      { path: 'estrategias', component: EstrategiasComponent },
      { path: 'gold', component: SuscripcionComponent },
      { path: 'pagos-success', component: PagosComponent },
      { path: 'pagos-cancel', component: PagosComponent },
    ]
  },

  // ruta final corregida
  { path: '**', redirectTo: 'inicio' }
];



@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
