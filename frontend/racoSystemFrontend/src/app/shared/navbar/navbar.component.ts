import { Component, OnInit } from '@angular/core';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit {
  isLoggedIn = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.isLoggedIn = this.authService.isAuthenticated();
  }

  loginGoogle(): void {
    this.authService.loginWithGoogle().then(() => {
      this.isLoggedIn = true;
    });
  }

  logout(): void {
    this.authService.logout();
    this.isLoggedIn = false;
  }
}