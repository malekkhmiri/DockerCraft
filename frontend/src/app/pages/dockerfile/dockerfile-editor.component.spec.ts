import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DockerfileEditorComponent } from './dockerfile-editor.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { AuthService } from '../../core/services/auth.service';
import { of } from 'rxjs';

describe('DockerfileEditorComponent', () => {
  let component: DockerfileEditorComponent;
  let fixture: ComponentFixture<DockerfileEditorComponent>;
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, DockerfileEditorComponent],
      providers: [DockerfileService, AuthService]
    }).compileComponents();

    fixture = TestBed.createComponent(DockerfileEditorComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
  });

  it('should hide edit button if isReadOnly is true', () => {
    component.dockerfile = { id: 1, content: 'FROM node', isReadOnly: true, createdAt: '' };
    component.loading = false;
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement;
    const editBtn = compiled.querySelector('.card-actions button');
    // The button shouldn't be there or should be null if isReadOnly is true
    expect(editBtn).toBeNull();
  });

  it('should disable gitlab download for free users', () => {
    spyOn(authService, 'isPro').and.returnValue(false);
    component.dockerfile = { id: 1, content: 'FROM node', isReadOnly: false, createdAt: '' };
    component.loading = false;
    fixture.detectChanges();
    
    const compiled = fixture.nativeElement;
    const gitlabBtn = compiled.querySelectorAll('.download-list button')[1];
    expect(gitlabBtn.disabled).toBeTrue();
  });
});
