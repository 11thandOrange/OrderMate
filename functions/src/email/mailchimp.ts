/**
 * Mailchimp email integration
 * Issue #98: Send auto emails on app lifecycle events
 */

import * as functions from 'firebase-functions';

// Mailchimp Transactional API (Mandrill)
const mailchimp = require('@mailchimp/mailchimp_transactional');

interface EmailTemplate {
    templateName: string;
    subject: string;
}

/**
 * Email templates for different lifecycle events
 */
const TEMPLATES: Record<string, EmailTemplate> = {
    'welcome': {
        templateName: 'ordermate-welcome',
        subject: 'Welcome to OrderMate! 🎉'
    },
    'farewell': {
        templateName: 'ordermate-farewell',
        subject: "We're sorry to see you go"
    },
    'upgrade': {
        templateName: 'ordermate-upgrade',
        subject: 'Your OrderMate plan has been upgraded! 🚀'
    },
    'downgrade': {
        templateName: 'ordermate-downgrade',
        subject: 'Your OrderMate plan has changed'
    },
    'usage_alert': {
        templateName: 'ordermate-usage-alert',
        subject: 'OrderMate Usage Alert'
    },
    'payment_reminder': {
        templateName: 'ordermate-payment-reminder',
        subject: 'Your OrderMate payment is coming up'
    },
    'payment_late': {
        templateName: 'ordermate-payment-late',
        subject: 'Action Required: OrderMate payment overdue'
    }
};

/**
 * Send an email using Mailchimp Transactional (Mandrill)
 * 
 * @param email - Recipient email address
 * @param templateKey - Key from TEMPLATES object
 * @param mergeVars - Optional variables to merge into template
 */
export async function sendMailchimpEmail(
    email: string,
    templateKey: string,
    mergeVars?: Record<string, string>
): Promise<boolean> {
    const template = TEMPLATES[templateKey];
    if (!template) {
        console.error(`Unknown email template: ${templateKey}`);
        return false;
    }

    const apiKey = functions.config().mailchimp?.api_key;
    if (!apiKey) {
        console.error('Mailchimp API key not configured');
        return false;
    }

    try {
        const client = mailchimp(apiKey);
        
        const response = await client.messages.sendTemplate({
            template_name: template.templateName,
            template_content: [],
            message: {
                to: [{ email, type: 'to' }],
                subject: template.subject,
                from_email: 'support@ordermate.app',
                from_name: 'OrderMate',
                global_merge_vars: mergeVars 
                    ? Object.entries(mergeVars).map(([name, content]) => ({ name, content }))
                    : []
            }
        });

        console.log(`Email sent to ${email} using template ${templateKey}:`, response);
        return true;
    } catch (error) {
        console.error(`Failed to send email to ${email}:`, error);
        return false;
    }
}

/**
 * Send a plain email without template (for internal reports)
 */
export async function sendPlainEmail(
    to: string,
    subject: string,
    htmlContent: string
): Promise<boolean> {
    const apiKey = functions.config().mailchimp?.api_key;
    if (!apiKey) {
        console.error('Mailchimp API key not configured');
        return false;
    }

    try {
        const client = mailchimp(apiKey);
        
        const response = await client.messages.send({
            message: {
                to: [{ email: to, type: 'to' }],
                subject: subject,
                from_email: 'analytics@ordermate.app',
                from_name: 'OrderMate Analytics',
                html: htmlContent
            }
        });

        console.log(`Plain email sent to ${to}:`, response);
        return true;
    } catch (error) {
        console.error(`Failed to send plain email to ${to}:`, error);
        return false;
    }
}
