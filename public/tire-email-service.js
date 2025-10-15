/**
 * EmailJS Service for Tire Management System
 * Alternative to SendGrid - Frontend email service
 * 
 * Setup Instructions:
 * 1. Create account at https://emailjs.com
 * 2. Add Gmail service
 * 3. Create email template  
 * 4. Get Service ID, Template ID, and Public Key
 * 5. Replace the config values below
 */

class TireEmailService {
    constructor() {
        // 🔧 EmailJS Configuration - Replace with your values
        this.config = {
            serviceId: 'service_xxxxxxx',        // Replace with your Service ID
            templateId: 'template_xxxxxxx',      // Replace with your Template ID
            publicKey: 'your_public_key_here'   // Replace with your Public Key
        };

        // Manager email configuration
        this.managerEmail = 'slthrmanager@gmail.com';
        this.frontendUrl = 'https://tire-slt.vercel.app';
        
        // Initialize EmailJS
        this.initialize();
    }

    /**
     * Initialize EmailJS with public key
     */
    initialize() {
        try {
            if (typeof emailjs !== 'undefined') {
                emailjs.init(this.config.publicKey);
                console.log('✅ EmailJS initialized successfully');
            } else {
                console.error('❌ EmailJS library not loaded. Add script tag to HTML.');
            }
        } catch (error) {
            console.error('❌ EmailJS initialization failed:', error);
        }
    }

    /**
     * Send manager notification for new tire request
     * @param {Object} tireRequest - Tire request data
     * @returns {Promise<boolean>} - Success status
     */
    async sendManagerNotification(tireRequest) {
        try {
            console.log('📧 Sending EmailJS notification for request:', tireRequest.id || 'NEW');

            // Prepare email template parameters
            const templateParams = {
                // EmailJS template variables
                to_email: this.managerEmail,
                from_name: 'Tire Management System',
                
                // Tire request details
                vehicle_no: tireRequest.vehicleNo || 'N/A',
                user_section: tireRequest.userSection || 'N/A',
                tire_size: tireRequest.tireSize || 'N/A',
                existing_make: tireRequest.existingMake || 'N/A',
                no_of_tires: tireRequest.noOfTires || 'N/A',
                cost_center: tireRequest.costCenter || 'N/A',
                officer_service_no: tireRequest.officerServiceNo || 'N/A',
                email: tireRequest.email || 'N/A',
                comments: tireRequest.comments || 'No comments',
                
                // System details
                request_id: tireRequest.id || 'PENDING',
                request_date: new Date().toLocaleDateString(),
                request_time: new Date().toLocaleTimeString(),
                dashboard_link: `${this.frontendUrl}/manager`,
                
                // Email subject
                subject: `🚗 New Tire Request - ${tireRequest.vehicleNo || 'Unknown Vehicle'}`
            };

            // Send email via EmailJS
            const response = await emailjs.send(
                this.config.serviceId,
                this.config.templateId,
                templateParams
            );

            if (response.status === 200) {
                console.log('✅ EmailJS notification sent successfully:', response);
                return true;
            } else {
                console.error('❌ EmailJS unexpected response:', response);
                return false;
            }

        } catch (error) {
            console.error('❌ EmailJS notification failed:', error);
            
            // Detailed error logging for debugging
            if (error.status) {
                console.error('EmailJS Error Status:', error.status);
            }
            if (error.text) {
                console.error('EmailJS Error Text:', error.text);
            }
            
            return false;
        }
    }

    /**
     * Test EmailJS connection and configuration
     * @returns {Promise<boolean>} - Test result
     */
    async testConnection() {
        try {
            console.log('🧪 Testing EmailJS connection...');

            const testParams = {
                to_email: this.managerEmail,
                from_name: 'Tire Management System - Test',
                subject: '🧪 EmailJS Connection Test',
                vehicle_no: 'TEST-VEHICLE-001',
                user_section: 'IT Department',
                tire_size: '205/55R16',
                request_date: new Date().toLocaleDateString(),
                request_time: new Date().toLocaleTimeString(),
                request_id: 'TEST-' + Date.now(),
                dashboard_link: `${this.frontendUrl}/manager`,
                comments: 'This is a test email to verify EmailJS configuration is working properly.'
            };

            const response = await emailjs.send(
                this.config.serviceId,
                this.config.templateId,
                testParams
            );

            console.log('✅ EmailJS test successful:', response);
            return true;

        } catch (error) {
            console.error('❌ EmailJS test failed:', error);
            return false;
        }
    }

    /**
     * Update EmailJS configuration
     * @param {Object} newConfig - New configuration object
     */
    updateConfig(newConfig) {
        this.config = { ...this.config, ...newConfig };
        this.initialize();
        console.log('🔄 EmailJS configuration updated');
    }

    /**
     * Get current configuration status
     * @returns {Object} - Configuration status
     */
    getStatus() {
        const isConfigured = this.config.serviceId !== 'service_xxxxxxx' && 
                           this.config.templateId !== 'template_xxxxxxx' && 
                           this.config.publicKey !== 'your_public_key_here';

        return {
            configured: isConfigured,
            serviceId: this.config.serviceId,
            templateId: this.config.templateId,
            publicKeySet: this.config.publicKey !== 'your_public_key_here',
            managerEmail: this.managerEmail
        };
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = TireEmailService;
}

// Global instance for direct use
window.tireEmailService = new TireEmailService();

// Usage example:
/*
// Basic usage in form submission:
async function submitTireRequest(formData) {
    try {
        // Submit to backend first
        const response = await fetch('/api/tire-requests', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(formData)
        });
        
        const result = await response.json();
        
        if (result.success) {
            // Send email notification via EmailJS
            const emailSent = await window.tireEmailService.sendManagerNotification(result.data);
            
            if (emailSent) {
                alert('✅ Tire request submitted and manager notified via EmailJS!');
            } else {
                alert('✅ Tire request submitted (email notification may have failed)');
            }
        }
    } catch (error) {
        console.error('Form submission error:', error);
        alert('❌ Failed to submit tire request');
    }
}

// Configuration update:
window.tireEmailService.updateConfig({
    serviceId: 'service_abc123',
    templateId: 'template_xyz789', 
    publicKey: 'your_actual_public_key'
});

// Test connection:
window.tireEmailService.testConnection()
    .then(success => console.log('Test result:', success));
*/