import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './register.component.html',
    styleUrl: './register.component.scss'
})
export class RegisterComponent {
    private readonly auth = inject(AuthService);
    private readonly router = inject(Router);

    username = '';
    email = '';
    password = '';
    isSubmitting = signal(false);
    error = signal<string | null>(null);

    submit(): void {
        this.isSubmitting.set(true);
        this.error.set(null);

        this.auth.register({ username: this.username, email: this.email, password: this.password }).subscribe({
            next: () => this.router.navigate(['/']),
            error: (err) => {
                // Backend returns 400 on validation fail (e.g. username taken, password too short)
                this.error.set(err.error?.message ?? 'Registration failed. Check your details.');
                this.isSubmitting.set(false);
            }
        });
    }
}