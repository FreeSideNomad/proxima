// LCARS UI JavaScript

class ProximaUI {
    constructor() {
        this.audioContext = null;
        this.soundEnabled = true;
        this.init();
    }

    init() {
        this.initAudio();
        this.setupEventListeners();
        this.loadCurrentConfig();
        this.startStatusUpdates();
    }

    initAudio() {
        try {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
        } catch (error) {
            console.log('Web Audio API not supported');
            this.soundEnabled = false;
        }
    }

    setupEventListeners() {
        // Add LCARS sounds to all buttons and clickable elements
        document.addEventListener('click', (e) => {
            // Play sound for LCARS buttons and nav items
            if (e.target.classList.contains('lcars-button') ||
                e.target.classList.contains('lcars-nav-item') ||
                e.target.classList.contains('activate-preset')) {
                this.playLCARSSound();
            }

            // Preset activation buttons
            if (e.target.classList.contains('activate-preset')) {
                e.preventDefault();
                this.activatePreset(e.target.dataset.preset);
            }
        });

        // Header form submissions
        const headerForms = document.querySelectorAll('.header-form');
        headerForms.forEach(form => {
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                this.playLCARSSound();
                this.saveHeaders(form);
            });
        });

        // Navigation active state
        const navItems = document.querySelectorAll('.lcars-nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', () => {
                navItems.forEach(nav => nav.classList.remove('active'));
                item.classList.add('active');
            });
        });

        // Resume audio context on first user interaction (required by browsers)
        document.addEventListener('click', () => {
            if (this.audioContext && this.audioContext.state === 'suspended') {
                this.audioContext.resume();
            }
        }, { once: true });
    }

    async loadCurrentConfig() {
        try {
            const response = await fetch('/proxima/api/config/info');
            const config = await response.json();
            this.updateConfigDisplay(config);
        } catch (error) {
            this.showAlert('Error loading configuration', 'error');
        }
    }

    async activatePreset(presetName) {
        try {
            const response = await fetch(`/proxima/api/config/presets/${presetName}/activate`, {
                method: 'POST'
            });

            const result = await response.json();

            if (result.status === 'success') {
                this.playLCARSSound('confirm');
                this.showAlert(`Preset "${presetName}" activated successfully`, 'success');
                this.loadCurrentConfig();
                this.updatePresetButtons(presetName);
                this.updatePresetStatusIndicators(presetName);

                // Reload the page after a short delay to ensure all UI elements reflect the new state
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            } else {
                this.playLCARSSound('error');
                this.showAlert(result.message, 'error');
            }
        } catch (error) {
            this.playLCARSSound('error');
            this.showAlert('Error activating preset', 'error');
        }
    }

    updatePresetButtons(activePreset) {
        const buttons = document.querySelectorAll('.activate-preset');
        buttons.forEach(button => {
            if (button.dataset.preset === activePreset) {
                button.textContent = 'ACTIVE';
                button.classList.add('success');
                button.disabled = true;
            } else {
                button.textContent = 'ACTIVATE';
                button.classList.remove('success');
                button.disabled = false;
            }
        });
    }

    updatePresetStatusIndicators(activePreset) {
        // Update all status indicators
        const statusIndicators = document.querySelectorAll('.lcars-status');
        statusIndicators.forEach(indicator => {
            const presetElement = indicator.closest('[data-preset-name]');
            if (presetElement) {
                const presetName = presetElement.dataset.presetName;
                if (presetName === activePreset) {
                    indicator.classList.remove('inactive');
                    indicator.classList.add('active');
                } else {
                    indicator.classList.remove('active');
                    indicator.classList.add('inactive');
                }
            }
        });

        // Also update buttons that might have changed state
        const allButtons = document.querySelectorAll('button.activate-preset, button[data-preset]');
        allButtons.forEach(button => {
            const presetName = button.dataset.preset;
            if (presetName === activePreset) {
                button.textContent = 'ACTIVE';
                button.classList.add('success');
                button.classList.remove('activate-preset');
                button.disabled = true;
            } else {
                button.textContent = 'ACTIVATE';
                button.classList.remove('success');
                button.classList.add('activate-preset');
                button.disabled = false;
            }
        });
    }

    updateConfigDisplay(config) {
        // Update active preset display
        const activePresetElement = document.getElementById('active-preset');
        if (activePresetElement) {
            activePresetElement.textContent = config.activePreset || 'None';
        }

        // Update downstream URL
        const downstreamElement = document.getElementById('downstream-url');
        if (downstreamElement) {
            downstreamElement.textContent = config.downstreamUrl;
        }

        // Update total presets count
        const totalPresetsElement = document.getElementById('total-presets');
        if (totalPresetsElement) {
            totalPresetsElement.textContent = config.totalPresets;
        }
    }

    async saveHeaders(form) {
        const formData = new FormData(form);
        const headers = {};

        for (let [key, value] of formData.entries()) {
            if (key.startsWith('header-')) {
                const headerName = key.replace('header-', '');
                headers[headerName] = value;
            }
        }

        try {
            // This would be implemented when we add header editing functionality
            this.showAlert('Header saving functionality coming soon', 'info');
        } catch (error) {
            this.showAlert('Error saving headers', 'error');
        }
    }

    showAlert(message, type = 'info') {
        const alertContainer = document.getElementById('alert-container');
        if (!alertContainer) return;

        const alert = document.createElement('div');
        alert.className = `lcars-alert ${type}`;
        alert.textContent = message;

        alertContainer.appendChild(alert);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (alert.parentNode) {
                alert.parentNode.removeChild(alert);
            }
        }, 5000);
    }

    startStatusUpdates() {
        // Update status every 30 seconds
        setInterval(() => {
            this.updateSystemStatus();
        }, 30000);

        // Initial status update
        this.updateSystemStatus();
    }

    async updateSystemStatus() {
        try {
            const response = await fetch('/actuator/health');
            const health = await response.json();

            const statusElement = document.getElementById('system-status');
            if (statusElement) {
                const isHealthy = health.status === 'UP';
                statusElement.className = `lcars-status ${isHealthy ? 'active' : 'inactive'}`;
                statusElement.title = `System Status: ${health.status}`;
            }
        } catch (error) {
            const statusElement = document.getElementById('system-status');
            if (statusElement) {
                statusElement.className = 'lcars-status inactive';
                statusElement.title = 'System Status: Unknown';
            }
        }
    }

    // LCARS sound effects implementation
    playLCARSSound(type = 'beep') {
        if (!this.soundEnabled || !this.audioContext) return;

        try {
            const oscillator = this.audioContext.createOscillator();
            const gainNode = this.audioContext.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(this.audioContext.destination);

            // Different LCARS sound types
            switch (type) {
                case 'beep':
                    // Classic LCARS beep: 800Hz for 120ms
                    oscillator.frequency.setValueAtTime(800, this.audioContext.currentTime);
                    gainNode.gain.setValueAtTime(0.1, this.audioContext.currentTime);
                    gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.12);
                    oscillator.start(this.audioContext.currentTime);
                    oscillator.stop(this.audioContext.currentTime + 0.12);
                    break;

                case 'confirm':
                    // Confirmation sound: rising tone
                    oscillator.frequency.setValueAtTime(600, this.audioContext.currentTime);
                    oscillator.frequency.exponentialRampToValueAtTime(1000, this.audioContext.currentTime + 0.1);
                    gainNode.gain.setValueAtTime(0.1, this.audioContext.currentTime);
                    gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.1);
                    oscillator.start(this.audioContext.currentTime);
                    oscillator.stop(this.audioContext.currentTime + 0.1);
                    break;

                case 'error':
                    // Error sound: low declining tone
                    oscillator.frequency.setValueAtTime(400, this.audioContext.currentTime);
                    oscillator.frequency.exponentialRampToValueAtTime(200, this.audioContext.currentTime + 0.15);
                    gainNode.gain.setValueAtTime(0.15, this.audioContext.currentTime);
                    gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.15);
                    oscillator.start(this.audioContext.currentTime);
                    oscillator.stop(this.audioContext.currentTime + 0.15);
                    break;
            }
        } catch (error) {
            console.log('LCARS sound playback error:', error);
        }
    }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new ProximaUI();
});

// Utility functions for LCARS animations
function animateLCARSPanel(element) {
    element.style.opacity = '0';
    element.style.transform = 'translateX(-20px)';

    setTimeout(() => {
        element.style.transition = 'all 0.5s ease';
        element.style.opacity = '1';
        element.style.transform = 'translateX(0)';
    }, 100);
}

// Auto-animate panels on load
document.addEventListener('DOMContentLoaded', () => {
    const panels = document.querySelectorAll('.lcars-panel');
    panels.forEach((panel, index) => {
        setTimeout(() => {
            animateLCARSPanel(panel);
        }, index * 200);
    });
});