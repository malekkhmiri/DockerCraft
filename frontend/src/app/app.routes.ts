import { Routes } from '@angular/router';

import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { UploadProjectComponent } from './pages/upload-project/upload-project.component';
import { DockerfileComponent } from './pages/dockerfile/dockerfile.component';
import { SignupComponent } from './pages/signup/signup.component';
import { AdminDashboardComponent } from './pages/admin-dashboard/admin-dashboard.component';
import { UsersManagementComponent } from './pages/users-management/users-management.component';
import { ProjectsManagementComponent } from './pages/projects-management/projects-management.component';
import { ProjectDetailComponent } from './pages/project-detail/project-detail.component';
import { AdminProjectDetailComponent } from './pages/admin-project-detail/admin-project-detail.component';
import { VerifyEmailComponent } from './pages/verify-email/verify-email.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { AdminDockerfilesManagementComponent } from './pages/admin-dockerfiles-management/admin-dockerfiles-management.component';
import { HomeComponent } from './pages/home/home.component';
import { StudentVerificationComponent } from './pages/student-verification/student-verification.component';
import { DockerfileEditorComponent } from './pages/dockerfile/dockerfile-editor.component';

import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'student-verification', component: StudentVerificationComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard] },
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [roleGuard],
    data: { role: 'ADMIN' }
  },
  {
    path: 'admin/users',
    component: UsersManagementComponent,
    canActivate: [roleGuard],
    data: { role: 'ADMIN' }
  },
  {
    path: 'admin/projects',
    component: ProjectsManagementComponent,
    canActivate: [roleGuard],
    data: { role: 'ADMIN' }
  },
  {
    path: 'admin/project/:id',
    component: AdminProjectDetailComponent,
    canActivate: [roleGuard],
    data: { role: 'ADMIN' }
  },
  {
    path: 'admin/dockerfiles',
    component: AdminDockerfilesManagementComponent,
    canActivate: [roleGuard],
    data: { role: 'ADMIN' }
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'upload',
    component: UploadProjectComponent,
    canActivate: [authGuard]
  },
  {
    path: 'project/:id',
    component: ProjectDetailComponent,
    canActivate: [authGuard]
  },
  {
    path: 'dockerfile',
    component: DockerfileComponent,
    canActivate: [authGuard]
  },
  {
    path: 'dockerfile/:projectId',
    component: DockerfileComponent,
    canActivate: [authGuard]
  },
  {
    path: 'dockerfile-editor/:projectId',
    component: DockerfileEditorComponent,
    canActivate: [authGuard]
  },
  { path: '**', component: NotFoundComponent }
];
