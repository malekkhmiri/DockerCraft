import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-verify-email',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterLink],
    templateUrl: './verify-email.component.html',
    styleUrls: ['./verify-email.component.css']
})
export class VerifyEmailComponent implements OnInit {
    verifyForm!: FormGroup;
    email = '';
    loading = false;
    resendLoading = false;
    submitted = false;
    errorMessage = '';
    successMessage = '';
    countdown = 0;
    private countdownInterval: any;

    constructor(
        private fb: FormBuilder,
        private route: ActivatedRoute,
        private authService: AuthService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.email = this.route.snapshot.queryParamMap.get('email') || '';

        this.verifyForm = this.fb.group({
            d1: ['', [Validators.required, Validators.pattern('[0-9]')]],
            d2: ['', [Validators.required, Validators.pattern('[0-9]')]],
            d3: ['', [Validators.required, Validators.pattern('[0-9]')]],
            d4: ['', [Validators.required, Validators.pattern('[0-9]')]],
            d5: ['', [Validators.required, Validators.pattern('[0-9]')]],
            d6: ['', [Validators.required, Validators.pattern('[0-9]')]]
        });
    }

    get f() { return this.verifyForm.controls; }

    // Naviguer automatiquement entre les chiffres
    onDigitInput(event: Event, nextId: string | null, prevId: string | null) {
        const input = event.target as HTMLInputElement;
        const value = input.value;

        if (value.length === 1 && nextId) {
            document.getElementById(nextId)?.focus();
        }
        if (value === '' && prevId) {
            document.getElementById(prevId)?.focus();
        }
    }

    onKeyDown(event: KeyboardEvent, prevId: string | null) {
        const input = event.target as HTMLInputElement;
        if (event.key === 'Backspace' && input.value === '' && prevId) {
            document.getElementById(prevId)?.focus();
        }
    }

    getCode(): string {
        return `${this.f['d1'].value}${this.f['d2'].value}${this.f['d3'].value}${this.f['d4'].value}${this.f['d5'].value}${this.f['d6'].value}`;
    }

    onSubmit() {
        this.submitted = true;
        this.errorMessage = '';
        if (this.verifyForm.invalid) return;

        this.loading = true;
        const code = this.getCode();

        this.authService.verifyEmail(this.email, code).subscribe({
            next: (response) => {
                this.loading = false;
                this.authService.saveUserData(response);
                if (response.role === 'ADMIN') {
                    this.router.navigate(['/admin/dashboard']);
                } else {
                    this.router.navigate(['/dashboard']);
                }
            },
            error: (err) => {
                this.loading = false;
                this.errorMessage = err.error?.message || 'Code incorrect. Veuillez réessayer.';
            }
        });
    }

    resendCode() {
        if (this.countdown > 0) return;
        this.resendLoading = true;
        this.errorMessage = '';

        this.authService.resendOtp(this.email).subscribe({
            next: () => {
                this.resendLoading = false;
                this.successMessage = 'Un nouveau code a été envoyé !';
                this.startCountdown(60);
                setTimeout(() => this.successMessage = '', 4000);
            },
            error: () => {
                this.resendLoading = false;
                this.errorMessage = 'Erreur lors du renvoi du code.';
            }
        });
    }

    private startCountdown(seconds: number) {
        this.countdown = seconds;
        clearInterval(this.countdownInterval);
        this.countdownInterval = setInterval(() => {
            this.countdown--;
            if (this.countdown <= 0) clearInterval(this.countdownInterval);
        }, 1000);
    }
}
