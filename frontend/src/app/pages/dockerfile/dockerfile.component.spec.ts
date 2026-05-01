import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DockerfileComponent } from './dockerfile.component';

describe('DockerfileComponent', () => {
  let component: DockerfileComponent;
  let fixture: ComponentFixture<DockerfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DockerfileComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DockerfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
