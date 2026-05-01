export interface Dockerfile {
  id: number;
  projectId: number;
  content: string;
  isReadOnly: boolean;
  createdAt?: string;
}

export interface UploadResponse {
  projectId: number;
  generationsUsedThisMonth: number;
  generationsLimit: number;
}

export interface DockerfileDto {
  id: number;
  content: string;
  isReadOnly: boolean;
  createdAt: string;
}

export interface QuotaDto {
  generationsUsedThisMonth: number;
  generationsLimit: number;
}
