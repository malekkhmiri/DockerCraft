import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UploadProjectComponent } from './upload-project.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { DockerfileService } from '../../core/services/dockerfile.service';
import { of } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';

describe('UploadProjectComponent', () => {
  let component: UploadProjectComponent;
  let fixture: ComponentFixture<UploadProjectComponent>;
  let dockerfileService: DockerfileService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, ReactiveFormsModule, UploadProjectComponent],
      providers: [DockerfileService]
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjectComponent);
    component = fixture.componentInstance;
    dockerfileService = TestBed.inject(DockerfileService);
  });

  it('should disable upload button when quota is reached', () => {
    component.quota = { generationsUsedThisMonth: 5, generationsLimit: 5 };
    fixture.detectChanges();
    
    expect(component.isQuotaReached()).toBeTrue();
    const compiled = fixture.nativeElement;
    const button = compiled.querySelector('button[type="submit"]');
    expect(button.disabled).toBeTrue();
  });

  it('should show orange badge when quota is 4/5', () => {
    component.quota = { generationsUsedThisMonth: 4, generationsLimit: 5 };
    component.loadingQuota = false;
    fixture.detectChanges();
    
    expect(component.getQuotaColor()).toBe('text-warning');
  });

  it('should show green badge when quota is 0/5', () => {
    component.quota = { generationsUsedThisMonth: 0, generationsLimit: 5 };
    component.loadingQuota = false;
    fixture.detectChanges();
    
    expect(component.getQuotaColor()).toBe('text-success');
  });
});
