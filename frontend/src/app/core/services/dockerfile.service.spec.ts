import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { DockerfileService } from './dockerfile.service';
import { environment } from '../../../environments/environment';

describe('DockerfileService', () => {
  let service: DockerfileService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DockerfileService]
    });
    service = TestBed.inject(DockerfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should upload project', () => {
    const mockFile = new File([''], 'test.zip');
    const mockResponse = { projectId: 1, generationsUsedThisMonth: 1, generationsLimit: 5 };

    service.uploadProject(mockFile).subscribe(res => {
      expect(res.projectId).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/projects/upload`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });

  it('should get user quota', () => {
    const mockQuota = { generationsUsedThisMonth: 3, generationsLimit: 5 };

    service.getUserQuota().subscribe(res => {
      expect(res.generationsUsedThisMonth).toBe(3);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/users/me/quota`);
    expect(req.request.method).toBe('GET');
    req.flush(mockQuota);
  });
});
