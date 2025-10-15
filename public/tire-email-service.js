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
            serviceId: 'service_xxxxxxx',        // Replace with your Service ID from EmailJS
            publicKey: 'your_public_key_here',   // Replace with your Public Key from EmailJS
            templates: {
                manager: 'template_manager',        // Manager notification template
                tto: 'template_tto',               // TTO notification template  
                engineer: 'template_engineer',     // Engineer notification template
                userFinal: 'template_user_final'   // User final notification template
            }
        };

        // Email configuration from application.properties
        this.managerEmail = 'slthrmanager@gmail.com';
        this.ttoEmail = 'slttransportofficer@gmail.com';
        this.engineerEmail = 'engineerslt38@gmail.com';
        this.frontendUrl = 'https://tire-frontend-main.vercel.app';
        
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

            // Send email via EmailJS - Step 1: Manager notification
            const response = await emailjs.send(
                this.config.serviceId,
                this.config.templates.manager,
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
     * Send TTO notification (Step 2)
     * @param {Object} tireRequest - Tire request data
     * @param {string} approvedBy - Manager who approved
     * @returns {Promise<boolean>} - Success status
     */
    async sendTTONotification(tireRequest, approvedBy = 'HR Manager') {
        try {
            console.log('📧 Sending TTO notification for request:', tireRequest.id);

            const templateParams = {
                to_email: 'tto@slt.lk',
                from_name: 'Tire Management System',
                subject: `🎯 Manager Approved - TTO Review Required - ${tireRequest.vehicleNo}`,
                vehicle_no: tireRequest.vehicleNo,
                user_section: tireRequest.userSection,
                tire_size: tireRequest.tireSize,
                request_id: tireRequest.id,
                approved_by: approvedBy,
                approval_date: new Date().toLocaleDateString(),
                dashboard_link: `${this.frontendUrl}/tto`
            };

            const response = await emailjs.send(
                this.config.serviceId,
                this.config.templates.tto,
                templateParams
            );

            console.log('✅ TTO notification sent successfully:', response);
            return response.status === 200;

        } catch (error) {
            console.error('❌ TTO notification failed:', error);
            return false;
        }
    }

    /**
     * Send Engineer notification (Step 3)
     * @param {Object} tireRequest - Tire request data
     * @param {string} approvedBy - TTO officer who approved
     * @returns {Promise<boolean>} - Success status
     */
    async sendEngineerNotification(tireRequest, approvedBy = 'TTO Officer') {
        try {
            console.log('📧 Sending Engineer notification for request:', tireRequest.id);

            const templateParams = {
                to_email: 'engineer@slt.lk',
                from_name: 'Tire Management System',
                subject: `⚙️ TTO Approved - Engineering Review Required - ${tireRequest.vehicleNo}`,
                vehicle_no: tireRequest.vehicleNo,
                user_section: tireRequest.userSection,
                tire_size: tireRequest.tireSize,
                request_id: tireRequest.id,
                approved_by: approvedBy,
                tto_approval_date: new Date().toLocaleDateString(),
                dashboard_link: `${this.frontendUrl}/engineer`
            };

            const response = await emailjs.send(
                this.config.serviceId,
                this.config.templates.engineer,
                templateParams
            );

            console.log('✅ Engineer notification sent successfully:', response);
            return response.status === 200;

        } catch (error) {
            console.error('❌ Engineer notification failed:', error);
            return false;
        }
    }

    /**
     * Send User final notification (Step 4)
     * @param {Object} tireRequest - Tire request data  
     * @param {string} approvedBy - Engineer who approved
     * @returns {Promise<boolean>} - Success status
     */
    async sendUserFinalNotification(tireRequest, approvedBy = 'Engineering Team') {
        try {
            console.log('📧 Sending User final notification for request:', tireRequest.id);

            const templateParams = {
                to_email: tireRequest.email,
                from_name: 'Tire Management System',
                subject: `✅ Tire Request Approved - Order Processing - ${tireRequest.vehicleNo}`,
                vehicle_no: tireRequest.vehicleNo,
                user_section: tireRequest.userSection,
                tire_size: tireRequest.tireSize,
                request_id: tireRequest.id,
                approved_by: approvedBy,
                final_approval_date: new Date().toLocaleDateString(),
                estimated_delivery: '7-10 business days',
                dashboard_link: `${this.frontendUrl}/orders`
            };

            const response = await emailjs.send(
                this.config.serviceId,
                this.config.templates.userFinal,
                templateParams
            );

            console.log('✅ User final notification sent successfully:', response);
            return response.status === 200;

        } catch (error) {
            console.error('❌ User final notification failed:', error);
            return false;
        }
    }

    /**
     * Complete workflow test - All 4 steps
     * @param {Object} sampleRequest - Sample tire request data
     * @returns {Promise<Object>} - Test results
     */
    async testCompleteWorkflow(sampleRequest) {
        const results = {
            step1: false,
            step2: false, 
            step3: false,
            step4: false,
            overall: false
        };

        try {
            console.log('🧪 Testing complete EmailJS workflow...');

            // Step 1: Manager notification
            results.step1 = await this.sendManagerNotification(sampleRequest);
            await this.delay(2000);

            // Step 2: TTO notification
            results.step2 = await this.sendTTONotification(sampleRequest, 'Test Manager');
            await this.delay(2000);

            // Step 3: Engineer notification
            results.step3 = await this.sendEngineerNotification(sampleRequest, 'Test TTO Officer');
            await this.delay(2000);

            // Step 4: User final notification
            results.step4 = await this.sendUserFinalNotification(sampleRequest, 'Test Engineer');

            results.overall = results.step1 && results.step2 && results.step3 && results.step4;
            
            console.log('🧪 Complete workflow test results:', results);
            return results;

        } catch (error) {
            console.error('❌ Complete workflow test failed:', error);
            return results;
        }
    }

    /**
     * Helper delay function
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
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