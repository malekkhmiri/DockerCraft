import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Client, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class WebSocketService {
    private stompClient: Client | null = null;
    private pipelineSubject = new Subject<any>();

    constructor(@Inject(PLATFORM_ID) private platformId: Object) {
        if (isPlatformBrowser(this.platformId)) {
            this.initConnection();
        }
    }

    private initConnection() {
        const socket = new SockJS(environment.wsUrl);
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            debug: (msg: any) => console.log('STOMP DEBUG:', msg),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000
        });

        this.stompClient.onConnect = (frame: any) => {
            console.log('Connected to WebSocket!');
            this.stompClient?.subscribe('/topic/pipelines', (message: any) => {
                this.pipelineSubject.next(JSON.parse(message.body));
            });
        };

        this.stompClient.activate();
    }

    watchPipelines(): Observable<any> {
        return this.pipelineSubject.asObservable();
    }

    watchProject(projectId: number): Observable<any> {
        const projectSubject = new Subject<any>();
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.subscribe(`/topic/project/${projectId}`, (message: any) => {
                projectSubject.next(JSON.parse(message.body));
            });
        } else if (this.stompClient) {
            this.stompClient.onConnect = (frame: any) => {
                this.stompClient?.subscribe(`/topic/project/${projectId}`, (message: any) => {
                    projectSubject.next(JSON.parse(message.body));
                });
            }
        }
        return projectSubject.asObservable();
    }
}
