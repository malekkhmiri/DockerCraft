export interface Pipeline {
    id: number;
    projectId: number;
    gitlabPipelineId?: string;
    status: string;
    createdAt: string;
    finishedAt?: string;
}
