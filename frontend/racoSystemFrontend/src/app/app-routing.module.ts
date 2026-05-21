import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { QueOfrecemosComponent } from './features/home/pages/que-ofrecemos/que-ofrecemos.component';
import { SuscripcionComponent } from './features/home/pages/notificaciones/suscripcion.component';
import { EstrategiasComponent } from './features/home/pages/funcionamiento/estrategias.component';
import { LoginComponent } from './features/login/login.component';
import { AuthGuard } from './core/guards/auth.guard';
import { DashboardUsuarioComponent } from './features/dashboard-usuario/dashboard-usuario.component';
import { DashboardInicioComponent } from './features/dashboard-usuario/inicio/inicio.component';
import { ContratosComponent } from './features/dashboard-usuario/contratos/contratos.component';
import { ChatbotComponent } from './features/dashboard-usuario/chatbot/chatbot.component';
import { NotificacionesComponent } from './features/dashboard-usuario/notificaciones/notificaciones.component';
import { PagosComponent } from './features/dashboard-usuario/pagos/pagos.component';

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
      { path: 'login', component: LoginComponent }
    ]
  },
  {
    path: 'dashboard',
    component: DashboardUsuarioComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: DashboardInicioComponent },
      { path: 'contratos', component: ContratosComponent },
      { path: 'chatbot', component: ChatbotComponent },
      { path: 'notificaciones', component: NotificacionesComponent },
      { path: 'pagos', component: PagosComponent },
      { path: 'perfil', component: DashboardInicioComponent }
    ]
  },
  { path: '**', redirectTo: 'inicio' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
