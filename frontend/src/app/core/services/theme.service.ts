import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

@Injectable({
    providedIn: 'root'
})
export class ThemeService {
    private themeSubject = new BehaviorSubject<string>('dark');
    theme$ = this.themeSubject.asObservable();

    constructor(@Inject(PLATFORM_ID) private platformId: Object) {
        if (isPlatformBrowser(this.platformId)) {
            const savedTheme = localStorage.getItem('theme') || 'dark';
            this.setTheme(savedTheme);
        }
    }

    setTheme(theme: string) {
        if (isPlatformBrowser(this.platformId)) {
            localStorage.setItem('theme', theme);
            document.body.className = theme === 'light' ? 'light-theme' : 'dark-theme';
            this.themeSubject.next(theme);
        }
    }

    toggleTheme() {
        const nextTheme = this.themeSubject.value === 'dark' ? 'light' : 'dark';
        this.setTheme(nextTheme);
    }

    getCurrentTheme(): string {
        return this.themeSubject.value;
    }
}
