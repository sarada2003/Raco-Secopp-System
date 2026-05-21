import { Component, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements AfterViewInit {
  @ViewChild('videoRef') videoRef!: ElementRef<HTMLVideoElement>;

  constructor(private router: Router) {}

  ngAfterViewInit() {
    const video = this.videoRef.nativeElement;
    video.muted = true;
    video.play().catch(() => {});
  }

  goToRegister() {
    this.router.navigate(['/register']);
  }
}
