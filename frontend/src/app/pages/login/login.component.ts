import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

import { AuthResponse } from '../../core/models/auth-response.model';

// Validateur pour domaines interdits (cohérence avec signup)
function forbiddenEmailDomain(domains: string[]) {
  return (control: any) => {
    const email = control.value;
    if (email && email.includes('@')) {
      const domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
      if (domains.includes(domain)) {
        return { forbiddenDomain: true };
      }
    }
    return null;
  };
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';

  forbiddenDomains = ['yopmail.com', 'mailinator.com', 'test.com', 'example.com', 'fake.com', 'tempmail.com'];

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email, forbiddenEmailDomain(this.forbiddenDomains)]],
      password: ['', Validators.required]
    });
  }

  get f() { return this.loginForm.controls; }

  onSubmit() {
    this.submitted = true;
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    const userData = {
      email: this.loginForm.value.email,
      password: this.loginForm.value.password
    };

    this.authService.login(userData).subscribe({
      next: (response: AuthResponse) => {
        this.authService.saveUserData(response);
        this.loading = false;

        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin/dashboard']);
        } else if (response.userType === 'STUDENT' && !response.isStudentVerified) {
          // Étudiant non vérifié -> Oblige à uploader la carte
          this.router.navigate(['/student-verification']);
        } else {
          // Tout autre cas validé (Worker, Normal ou Étudiant déjà OK)
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Erreur lors de la connexion.';
        
        // Si l'erreur concerne la vérification de l'email, on peut proposer une redirection ou le faire auto
        if (this.errorMessage.includes('vérifier votre email')) {
          setTimeout(() => {
            this.router.navigate(['/verify-email'], { queryParams: { email: this.loginForm.value.email } });
          }, 2000);
        }
        console.error(err);
      }
    });
  }


  loginWithGoogle(): void {
    console.log('Google login');
  }

  loginWithGithub(): void {
    console.log('GitHub login');
  }
}
