import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

function mustMatch(controlName: string, matchingControlName: string) {
  return (formGroup: any) => {
    const control = formGroup.controls[controlName];
    const matchingControl = formGroup.controls[matchingControlName];
    if (matchingControl.errors && !matchingControl.errors['mustMatch']) return;
    if (control.value !== matchingControl.value) {
      matchingControl.setErrors({ mustMatch: true });
    } else {
      matchingControl.setErrors(null);
    }
  };
}

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
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent implements OnInit {
  signupForm!: FormGroup;
  loading = false;
  submitted = false;
  errorMessage = '';

  forbiddenDomains = ['yopmail.com', 'mailinator.com', 'test.com', 'example.com', 'fake.com', 'tempmail.com'];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
    this.signupForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email, forbiddenEmailDomain(this.forbiddenDomains)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
      acceptTerms: [false, Validators.requiredTrue]
    }, { validator: mustMatch('password', 'confirmPassword') });
  }

  get f() { return this.signupForm.controls; }

  onSubmit() {
    this.submitted = true;
    if (this.signupForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    const userData = {
      username: this.signupForm.value.username,
      email: this.signupForm.value.email,
      password: this.signupForm.value.password,
    };

    this.authService.signup(userData).subscribe({
      next: () => {
        this.loading = false;
        // Après le signup, on redirige vers la vérification d'email
        this.router.navigate(['/verify-email'], { 
          queryParams: { email: this.signupForm.value.email } 
        });
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          // Compte vérifié existant : afficher message clair
          this.errorMessage = 'Un compte actif existe déjà avec cet email. Veuillez vous connecter ou utiliser "Mot de passe oublié".';
        } else if (err.status === 0) {
          this.errorMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
        } else {
          this.errorMessage = err.error?.message || "Erreur lors de l'inscription.";
        }
      }
    });
  }


  signUpWithGoogle() { }
  signUpWithGithub() { }
}
