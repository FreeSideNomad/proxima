// LCARS UI JavaScript

class ProximaUI {
    constructor() {
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.loadCurrentConfig();
        this.startStatusUpdates();
    }

    setupEventListeners() {
        // Preset activation buttons
        document.addEventListener('click', (e) => {
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
                this.showAlert(`Preset "${presetName}" activated successfully`, 'success');
                this.loadCurrentConfig();
                this.updatePresetButtons(presetName);
            } else {
                this.showAlert(result.message, 'error');
            }
        } catch (error) {
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

    // Utility method for making LCARS sound effects (optional)
    playLCARSSound() {
        // Could implement Web Audio API sounds here
        console.log('*LCARS beep*');
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