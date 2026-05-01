import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/services/admin.service';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { AdminSidebarComponent } from '../../shared/admin-sidebar/admin-sidebar.component';

@Component({
    selector: 'app-users-management',
    standalone: true,
    imports: [CommonModule, RouterLink, FormsModule, AdminSidebarComponent],
    templateUrl: './users-management.component.html',
    styleUrls: ['./users-management.component.css']
})
export class UsersManagementComponent implements OnInit {
    users: any[] = [];
    loading = false;
    toasts: { id: number; message: string; type: string }[] = [];
    private toastId = 0;
    Math = Math; // For template usage

    // Filters
    searchTerm = '';
    roleFilter = '';
    statusFilter = '';
    currentPage = 1;
    pageSize = 8;

    // Selected user modal
    selectedUser: any = null;
    showModal = false;
    confirmAction: { type: 'delete' | 'toggle'; user: any } | null = null;

    // Stats
    get totalUsers() { return this.users.length; }
    get activeUsers() { return this.users.filter(u => u.active).length; }
    get adminCount() { return this.users.filter(u => u.role === 'ADMIN').length; }

    constructor(
        private adminService: AdminService,
        private authService: AuthService,
        private router: Router,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    getUsername() { 
        if (isPlatformBrowser(this.platformId)) {
            return localStorage.getItem('username') || 'Admin';
        }
        return 'Admin';
    }

    onLogout() {
        this.authService.logout();
        this.router.navigate(['/login']);
    }

    ngOnInit(): void { this.loadUsers(); }

    loadUsers() {
        this.loading = true;
        this.adminService.getUsers().subscribe({
            next: (data) => { this.users = data; this.loading = false; },
            error: () => this.loading = false
        });
    }

    deleteUser(id: number) {
        this.adminService.deleteUser(id).subscribe({
            next: () => {
                this.loadUsers();
                this.showModal = false;
                this.confirmAction = null;
                this.addToast('Utilisateur supprimé avec succès', 'success');
            },
            error: () => this.addToast('Erreur lors de la suppression', 'error')
        });
    }

    toggleStatus(user: any) {
        const newStatus = !user.active;
        this.adminService.toggleUserStatus(user.id, newStatus).subscribe({
            next: () => {
                user.active = newStatus;
                this.confirmAction = null;
                this.addToast(
                    `Compte ${user.username} ${newStatus ? 'activé' : 'désactivé'}`,
                    newStatus ? 'success' : 'info'
                );
            },
            error: () => this.addToast('Erreur lors du changement de statut', 'error')
        });
    }

    openConfirm(type: 'delete' | 'toggle', user: any): void {
        this.confirmAction = { type, user };
    }

    executeConfirm(): void {
        if (!this.confirmAction) return;
        if (this.confirmAction.type === 'delete') {
            this.deleteUser(this.confirmAction.user.id);
        } else {
            this.toggleStatus(this.confirmAction.user);
        }
    }

    viewUser(user: any): void {
        this.selectedUser = user;
        this.showModal = true;
    }

    get filteredUsers() {
        return this.users.filter(u => {
            const matchSearch = !this.searchTerm ||
                u.username?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
                u.email?.toLowerCase().includes(this.searchTerm.toLowerCase());
            const matchRole = !this.roleFilter || u.role === this.roleFilter;
            const matchStatus = this.statusFilter === '' ? true :
                this.statusFilter === 'active' ? u.active : !u.active;
            return matchSearch && matchRole && matchStatus;
        });
    }

    get paginatedUsers() {
        const start = (this.currentPage - 1) * this.pageSize;
        return this.filteredUsers.slice(start, start + this.pageSize);
    }

    get totalPages() { return Math.ceil(this.filteredUsers.length / this.pageSize); }
    get pageRange() { return Array.from({ length: this.totalPages }, (_, i) => i + 1); }
    goToPage(p: number) { if (p >= 1 && p <= this.totalPages) this.currentPage = p; }

    getBadgeClass(role: string) {
        return role === 'ADMIN' ? 'role-badge admin' : 'role-badge user';
    }

    getAvatarColor(username: string): string {
        const colors = ['blue', 'purple', 'cyan', 'green', 'amber', 'pink'];
        const code = username?.charCodeAt(0) || 0;
        return colors[code % colors.length];
    }

    removeToast(id: number): void {
        this.toasts = this.toasts.filter(t => t.id !== id);
    }

    addToast(message: string, type: string) {
        const id = ++this.toastId;
        this.toasts.push({ id, message, type });
        setTimeout(() => { this.toasts = this.toasts.filter(t => t.id !== id); }, 4000);
    }

    formatDate(date: string): string {
        if (!date) return '—';
        return new Date(date).toLocaleDateString('fr-FR', {
            day: '2-digit', month: 'short', year: 'numeric'
        });
    }

    viewUserProjects(username: string): void {
        this.showModal = false;
        this.router.navigate(['/admin/projects'], { queryParams: { user: username } });
    }
}
