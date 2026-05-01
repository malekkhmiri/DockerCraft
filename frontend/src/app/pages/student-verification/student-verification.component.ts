import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { StudentVerificationService } from '../../core/services/student-verification.service';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-student-verification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './student-verification.component.html',
  styleUrls: ['./student-verification.component.css']
})
export class StudentVerificationComponent implements OnInit {

  // ─── State ───────────────────────────────────────────────────────────────
  userId: number | null = null;
  selectedFile: File | null = null;
  previewUrl: string | null = null;
  loading = false;
  errorMessage = '';
  verificationResult: any = null;
  isDragOver = false;

  constructor(
    private verificationService: StudentVerificationService,
    private authService: AuthService,
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      // Essai 1 : userId directement en localStorage
      const storedId = localStorage.getItem('userId');
      if (storedId) {
        this.userId = parseInt(storedId, 10);
        console.log('[StudentVerification] userId from localStorage:', this.userId);
        this.checkExistingVerification();
        return;
      }

      // Essai 2 : récupérer l'userId depuis l'API via l'email (ancienne session)
      const email = localStorage.getItem('userEmail');
      if (email) {
        console.log('[StudentVerification] userId manquant, récupération via email:', email);
        this.fetchUserIdByEmail(email);
      } else {
        this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
      }
    }
  }

  /** Récupère l'ID utilisateur depuis /me si absent du localStorage (ancienne session) */
  private fetchUserIdByEmail(email: string): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const token = localStorage.getItem('token');
    if (!token) {
      this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
      return;
    }
    // /me = endpoint qui retourne l'utilisateur connecté via son JWT
    this.http.get<any>(`${environment.apiUrl}/users/me`, {
      headers: { Authorization: `Bearer ${token}` }
    }).subscribe({
      next: (user) => {
        if (user?.id) {
          this.userId = user.id;
          localStorage.setItem('userId', user.id.toString());
          console.log('[StudentVerification] userId récupéré via /me:', this.userId);
          this.checkExistingVerification();
        } else {
          this.errorMessage = 'Impossible de récupérer votre profil. Veuillez vous reconnecter.';
        }
      },
      error: (err) => {
        console.error('[StudentVerification] Erreur /me:', err);
        this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
      }
    });
  }

  // ─── File Selection ───────────────────────────────────────────────────────

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.setFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = true;
  }

  onDragLeave(): void {
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.setFile(file);
  }

  private setFile(file: File): void {
    const allowed = ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf'];
    if (!allowed.includes(file.type)) {
      this.errorMessage = 'Format non supporté. Utilisez JPG, PNG ou PDF.';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Le fichier ne doit pas dépasser 5 MB.';
      return;
    }
    this.selectedFile = file;
    this.errorMessage = '';

    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => this.previewUrl = e.target?.result as string;
      reader.readAsDataURL(file);
    } else {
      this.previewUrl = null;
    }
  }

  removeFile(): void {
    this.selectedFile = null;
    this.previewUrl = null;
    this.errorMessage = '';
  }

  // ─── Upload & Verify ─────────────────────────────────────────────────────

  onSubmit(): void {
    if (!this.selectedFile) {
      this.errorMessage = 'Veuillez sélectionner un fichier.';
      return;
    }
    if (!this.userId) {
      this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
      return;
    }

    console.log('[StudentVerification] Upload → userId:', this.userId, 'fichier:', this.selectedFile.name);
    this.loading = true;
    this.errorMessage = '';

    this.verificationService.uploadStudentCard(this.userId!, this.selectedFile).subscribe({
      next: (result) => {
        this.loading = false;
        console.log('[StudentVerification] Résultat IA:', result);
        this.verificationResult = result;
        if (isPlatformBrowser(this.platformId)) {
             if (result.status === 'AI_APPROVED' || result.status === 'ADMIN_APPROVED') {
                  localStorage.setItem('isStudentVerified', 'true');
             } else {
                  localStorage.setItem('isStudentVerified', 'false');
             }
        }
      },
      error: (err) => {
        this.loading = false;
        console.error('[StudentVerification] Erreur upload:', err);
        if (err.status === 0) {
          this.errorMessage = 'Serveur non accessible. Vérifiez que le backend est démarré (port 8080).';
        } else if (err.status === 413) {
          this.errorMessage = 'Fichier trop volumineux (max 5 MB).';
        } else if (err.status === 404) {
          this.errorMessage = 'Utilisateur non trouvé. Veuillez vous reconnecter.';
        } else {
          this.errorMessage = err.error?.message || `Erreur ${err.status || 'réseau'}. Veuillez réessayer.`;
        }
      }
    });
  }

  // ─── Navigation ───────────────────────────────────────────────────────────

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  skipVerification(): void {
    this.router.navigate(['/dashboard']);
  }

  reconnect(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.clear();
      this.router.navigate(['/login']);
    }
  }

  private checkExistingVerification(): void {
    if (!this.userId) return;
    this.verificationRepositoryMethod();
  }

  private verificationRepositoryMethod(): void {
     this.verificationService.getVerificationStatus(this.userId!).subscribe({
        next: (result) => {
          if (result.status === 'AI_APPROVED' || result.status === 'ADMIN_APPROVED') {
            this.router.navigate(['/dashboard']);
          }
        },
        error: () => { /* Pas de vérification existante → OK */ }
      });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'AI_APPROVED':    '✅ Approuvé automatiquement par IA',
      'ADMIN_APPROVED': '✅ Approuvé par l\'administrateur',
      'ADMIN_REVIEW':   '⏳ En cours de vérification manuelle (24-48h)',
      'REJECTED':       '❌ Document refusé',
      'AI_PROCESSING':  '🔄 Analyse IA en cours...',
      'PENDING':        '⏳ En attente',
      'EXPIRED':        '⚠️ Vérification expirée'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    if (status === 'AI_APPROVED' || status === 'ADMIN_APPROVED') return 'approved';
    if (status === 'REJECTED') return 'rejected';
    return 'pending';
  }

  getConfidencePercent(): number {
    return Math.round((this.verificationResult?.confidenceScore || 0) * 100);
  }

  isPdf(): boolean {
    return this.selectedFile?.type === 'application/pdf';
  }

  get isSubmitDisabled(): boolean {
    return !this.selectedFile || this.loading;
  }
}
