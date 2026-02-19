import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FlexLayoutModule } from '@angular/flex-layout';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NavbarComponent } from './shared/navbar/navbar.component';
import { MainLayoutComponent } from './layouts/main-layout/main-layout.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { ReactiveFormsModule } from '@angular/forms';
import { provideFirebaseApp, initializeApp } from '@angular/fire/app';
import { provideAuth, getAuth } from '@angular/fire/auth';
import { environment } from 'src/enviroments/environment';
import { LoginComponent } from './features/login/login.component';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { HttpClientModule } from '@angular/common/http';
import { DashboardUsuarioComponent } from './features/dashboard-usuario/dashboard-usuario.component';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { PagosComponent } from './features/dashboard-usuario/pagos/pagos.component';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    MainLayoutComponent,
    LoginComponent,
    DashboardUsuarioComponent,
    PagosComponent
  ],
  imports: [
    FlexLayoutModule,
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule ,
    MatToolbarModule, 
    MatButtonModule,
    ReactiveFormsModule,
     MatToolbarModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatButtonModule,
    MatMenuModule,
    FormsModule,
    MatIconModule,
    provideFirebaseApp(() => initializeApp(environment.firebaseConfig)),
    provideAuth(() => getAuth())
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }],
  bootstrap: [AppComponent]
})
export class AppModule { }
